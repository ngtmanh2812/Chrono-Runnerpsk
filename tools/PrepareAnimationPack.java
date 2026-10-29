import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.ImageOutputStream;
import chronorunner.gfx.PixelFont;

/** Technical preparation of image_gen atlases: slicing, common scale/pivot,
 * binary alpha, one shared palette per actor and actual animated previews.
 * Does not redraw poses, modify game assets or change gameplay code. */
public final class PrepareAnimationPack {
    static final Path ROOT = Path.of("art/animations/v2");
    static final String[] MOVE = {"idle","walk","run","jump","dash"};
    static final String[] COMBAT = {"attack","special","block","hurt","death"};
    static final String[] ORDER = {"idle","run","jump","attack","hurt","death","walk","special","block","dash","fall","land"};
    static final Set<String> LOOPS = Set.of("idle","walk","run","fall");
    static int checks;

    record Actor(String id,String label,boolean boss,boolean floating,int size,int bodyHeight,int previewScale) { }
    static final Actor[] ACTORS = {
        new Actor("phan_si_kien","Phan Sĩ Kiên",false,false,128,64,2),
        new Actor("ronaldo","Ronaldo",false,false,128,64,2),
        new Actor("do_mixi","Độ Mixi",false,false,128,64,2),
        new Actor("the_time_guardian","The Time Guardian",true,false,256,92,3),
        new Actor("the_void_core","The Void Core",true,true,256,105,3)
    };
    static final class Cell {
        final BufferedImage image;
        final Rectangle source, visible, body;
        double rootX, rootY;
        double sourceScale=1;
        String overrideFile;
        Cell(BufferedImage image,Rectangle source) {
            this.image=image; this.source=source;
            visible=bounds(image,false); body=bounds(image,true);
            require(visible!=null,"Empty source cell "+source);
            require(body!=null,"No character body in source cell "+source);
        }
    }
    static final class Sheet {
        String name; BufferedImage image; int[] rows; int[][] cols=new int[5][];
        Cell[][] cells=new Cell[5][6]; double referenceHeight,scale;
    }
    static final class Pack {
        Actor actor; Path directory;
        Sheet move, combat;
        int[] palette;
        Map<String,List<BufferedImage>> clips=new LinkedHashMap<>();
        int pivotX,pivotY;
        double safety;
    }

    static void require(boolean ok,String message) {
        checks++;
        if(!ok) throw new IllegalStateException(message);
    }
    static boolean body(int argb) {
        if((argb>>>24)<128) return false;
        int r=(argb>>16)&255,g=(argb>>8)&255,b=argb&255;
        if(r>70&&r>g*1.45&&r>b*1.15) return false; // trailing red cape/scarf
        if(g>110&&b>150&&r<170) return false; // cyan energy
        if(r>170&&g>90&&b<110) return false; // amber energy / bright trim
        if(b>170&&r>110&&g<110) return false; // violet energy
        return r+g+b<450;
    }
    static Rectangle bounds(BufferedImage image,boolean bodyOnly) {
        int x0=image.getWidth(),y0=image.getHeight(),x1=-1,y1=-1;
        for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) {
            int px=image.getRGB(x,y);
            if(bodyOnly ? body(px) : (px>>>24)>=128) {
                x0=Math.min(x0,x); y0=Math.min(y0,y); x1=Math.max(x1,x); y1=Math.max(y1,y);
            }
        }
        return x1<0?null:new Rectangle(x0,y0,x1-x0+1,y1-y0+1);
    }
    static int[] cuts(int[] projection,int count,int radius) {
        int[] out=new int[count+1]; out[count]=projection.length;
        for(int i=1;i<count;i++) {
            int expected=projection.length*i/count;
            long best=Long.MAX_VALUE; int choice=expected;
            int lo=Math.max(out[i-1]+8,expected-radius),hi=Math.min(projection.length-8,expected+radius);
            for(int y=lo;y<=hi;y++) {
                long sum=0;
                for(int k=-2;k<=2;k++) sum+=projection[Math.max(0,Math.min(projection.length-1,y+k))];
                long score=sum*10000+Math.abs(y-expected);
                if(score<best) { best=score; choice=y; }
            }
            out[i]=choice;
        }
        return out;
    }
    static double median(double... values) {
        double[] sorted=values.clone(); Arrays.sort(sorted);
        return sorted[sorted.length/2];
    }
    static Sheet readSheet(String actor,String name) throws Exception {
        Sheet sheet=new Sheet(); sheet.name=name;
        Path path=ROOT.resolve("source/"+actor+"-"+name+".png");
        BufferedImage raw=ImageIO.read(path.toFile());
        require(raw!=null&&raw.getColorModel().hasAlpha(),"Source needs true alpha: "+path);
        int w=raw.getWidth(),h=raw.getHeight();
        sheet.image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        int[] rowProjection=new int[h];
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            int value=raw.getRGB(x,y);
            int cleaned=(value>>>24)>=128?(0xff000000|(value&0xffffff)):0;
            sheet.image.setRGB(x,y,cleaned);
            if(cleaned!=0) rowProjection[y]++;
        }
        sheet.rows=cuts(rowProjection,5,(int)(h*.09));
        for(int row=0;row<5;row++) {
            int[] colProjection=new int[w];
            for(int y=sheet.rows[row];y<sheet.rows[row+1];y++) for(int x=0;x<w;x++) {
                int px=sheet.image.getRGB(x,y);
                if((px>>>24)!=0) colProjection[x]+=body(px)?17:1;
            }
            sheet.cols[row]=cuts(colProjection,6,(int)(w*.025));
            for(int col=0;col<6;col++) {
                Rectangle rect=new Rectangle(sheet.cols[row][col],sheet.rows[row],
                        sheet.cols[row][col+1]-sheet.cols[row][col],sheet.rows[row+1]-sheet.rows[row]);
                BufferedImage cell=new BufferedImage(rect.width,rect.height,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=cell.createGraphics(); g.drawImage(sheet.image,-rect.x,-rect.y,null); g.dispose();
                if(actor.equals("do_mixi")&&name.equals("locomotion")&&row==3&&col==2) {
                    Path extra=ROOT.resolve("source/do_mixi-jump-apex.png");
                    BufferedImage authored=ImageIO.read(extra.toFile());
                    for(int yy=0;yy<authored.getHeight();yy++)for(int xx=0;xx<authored.getWidth();xx++) {
                        int value=authored.getRGB(xx,yy);
                        authored.setRGB(xx,yy,(value>>>24)>=128?(0xff000000|(value&0xffffff)):0);
                    }
                    Cell replacement=new Cell(authored,new Rectangle(0,0,authored.getWidth(),authored.getHeight()));
                    // Calibrate a separately authored tucked pose to the same
                    // physical body size, with one uniform scale on both axes.
                    replacement.sourceScale=sheet.cells[0][0].body.height*.80/replacement.body.height;
                    replacement.overrideFile="art/animations/v2/source/do_mixi-jump-apex.png";
                    sheet.cells[row][col]=replacement;
                } else sheet.cells[row][col]=new Cell(cell,rect);
            }
        }
        if(name.equals("locomotion")) {
            double[] heights=new double[6];
            for(int i=0;i<6;i++) heights[i]=sheet.cells[0][i].body.height;
            sheet.referenceHeight=median(heights);
        } else sheet.referenceHeight=median(sheet.cells[0][5].body.height,sheet.cells[1][5].body.height,
                sheet.cells[2][5].body.height,sheet.cells[3][5].body.height);
        return sheet;
    }
    static double footX(Cell cell,double referenceHeight) {
        int bottom=cell.body.y+cell.body.height;
        int top=Math.max(cell.body.y,bottom-(int)Math.ceil(referenceHeight*.16));
        long sum=0,count=0;
        for(int y=top;y<bottom;y++) for(int x=0;x<cell.image.getWidth();x++) {
            if(body(cell.image.getRGB(x,y))) { sum+=x; count++; }
        }
        return count==0?cell.body.getCenterX():(double)sum/count;
    }
    static Pack prepare(Actor actor) throws Exception {
        Pack p=new Pack(); p.actor=actor;
        p.directory=ROOT.resolve((actor.boss?"bosses/":"players/")+actor.id);
        Files.createDirectories(p.directory);
        p.move=readSheet(actor.id,"locomotion"); p.combat=readSheet(actor.id,"combat");
        // Leave room beneath the feet for authored shockwaves and cape tips.
        // This padding is explicit in the runtime pivot, never added to physics.
        p.pivotX=actor.size/2; p.pivotY=actor.floating?actor.size/2:actor.size-(actor.boss?24:12);
        p.safety=1;
        for(Sheet sheet:List.of(p.move,p.combat)) {
            sheet.scale=actor.bodyHeight/sheet.referenceHeight;
            for(int row=0;row<5;row++) {
                // Final cell is the returned ready stance (or low debris in Death).
                double floor=sheet.cells[row][5].body.getMaxY();
                for(Cell cell:sheet.cells[row]) {
                    cell.rootX=actor.floating?cell.body.getCenterX():footX(cell,sheet.referenceHeight);
                    cell.rootY=actor.floating?floor-sheet.referenceHeight/2:cell.body.getMaxY();
                    double[] extents={cell.rootX-cell.visible.x,cell.visible.getMaxX()-cell.rootX,
                            cell.rootY-cell.visible.y,cell.visible.getMaxY()-cell.rootY};
                    double[] spaces={p.pivotX-3,actor.size-p.pivotX-3,p.pivotY-3,actor.size-p.pivotY-3};
                    for(int i=0;i<4;i++) if(extents[i]>0) p.safety=Math.min(p.safety,spaces[i]/(extents[i]*sheet.scale*cell.sourceScale));
                }
            }
        }
        for(Sheet sheet:List.of(p.move,p.combat)) {
            sheet.scale*=p.safety;
            String[] names=sheet==p.move?MOVE:COMBAT;
            for(int row=0;row<5;row++) {
                List<BufferedImage> clip=new ArrayList<>();
                for(Cell cell:sheet.cells[row]) {
                    BufferedImage frame=new BufferedImage(actor.size,actor.size,BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g=frame.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    double scale=sheet.scale*cell.sourceScale;
                    int dx=(int)Math.round(p.pivotX-cell.rootX*scale);
                    int dy=(int)Math.round(p.pivotY-cell.rootY*scale);
                    int width=(int)Math.round(cell.image.getWidth()*scale),height=(int)Math.round(cell.image.getHeight()*scale);
                    g.drawImage(cell.image,dx,dy,width,height,null); g.dispose(); clip.add(frame);
                }
                p.clips.put(names[row],clip);
            }
        }
        p.palette=buildPalette(p.clips.values().stream().flatMap(List::stream).toList(),64);
        for(List<BufferedImage> clip:p.clips.values()) for(BufferedImage frame:clip) applyPalette(frame,p.palette);
        p.clips.put("fall",List.of(p.clips.get("jump").get(2),p.clips.get("jump").get(3)));
        p.clips.put("land",List.of(p.clips.get("jump").get(4),p.clips.get("jump").get(5)));
        writePack(p); validate(p);
        return p;
    }

    static int key(int rgb) { return ((rgb>>19)&31)<<10|((rgb>>11)&31)<<5|((rgb>>3)&31); }
    static int channel(int key,int index) { return (key>>(10-index*5))&31; }
    static final class ColourBox {
        List<Integer> bins; long weight; int splitChannel,range;
        ColourBox(List<Integer> bins,int[] histogram) {
            this.bins=bins; int[] min={31,31,31},max={0,0,0};
            for(int bin:bins) { weight+=histogram[bin]; for(int c=0;c<3;c++) {
                int v=channel(bin,c); min[c]=Math.min(min[c],v); max[c]=Math.max(max[c],v);
            }}
            for(int c=0;c<3;c++) if(max[c]-min[c]>range) { range=max[c]-min[c];splitChannel=c; }
        }
    }
    static int[] buildPalette(List<BufferedImage> images,int count) {
        int[] histogram=new int[32768];
        for(BufferedImage image:images) for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) {
            int rgb=image.getRGB(x,y); if((rgb>>>24)!=0) histogram[key(rgb)]++;
        }
        List<Integer> used=new ArrayList<>();
        for(int i=0;i<histogram.length;i++) if(histogram[i]>0) used.add(i);
        List<ColourBox> boxes=new ArrayList<>(); boxes.add(new ColourBox(used,histogram));
        while(boxes.size()<count) {
            ColourBox box=boxes.stream().filter(b->b.bins.size()>1)
                    .max(Comparator.comparingDouble(b->b.range*Math.sqrt(b.weight))).orElse(null);
            if(box==null) break;
            int c=box.splitChannel; box.bins.sort(Comparator.comparingInt(bin->channel(bin,c)));
            long sum=0; int split=1;
            for(int i=0;i<box.bins.size()-1;i++) { sum+=histogram[box.bins.get(i)]; split=i+1; if(sum>=box.weight/2) break; }
            boxes.remove(box);
            boxes.add(new ColourBox(new ArrayList<>(box.bins.subList(0,split)),histogram));
            boxes.add(new ColourBox(new ArrayList<>(box.bins.subList(split,box.bins.size())),histogram));
        }
        int[] palette=new int[boxes.size()];
        for(int i=0;i<boxes.size();i++) {
            ColourBox b=boxes.get(i); long r=0,g=0,blue=0;
            for(int bin:b.bins) { long n=histogram[bin];r+=(channel(bin,0)*8+4)*n;g+=(channel(bin,1)*8+4)*n;blue+=(channel(bin,2)*8+4)*n; }
            palette[i]=0xff000000|((int)(r/b.weight)<<16)|((int)(g/b.weight)<<8)|(int)(blue/b.weight);
        }
        return palette;
    }
    static void applyPalette(BufferedImage image,int[] palette) {
        int[] cache=new int[32768]; Arrays.fill(cache,-1);
        for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) {
            int rgb=image.getRGB(x,y); if((rgb>>>24)==0) continue;
            int key=key(rgb),index=cache[key];
            if(index<0) {
                int best=Integer.MAX_VALUE; index=0;
                for(int i=0;i<palette.length;i++) {
                    int dr=((rgb>>16)&255)-((palette[i]>>16)&255),dg=((rgb>>8)&255)-((palette[i]>>8)&255),db=(rgb&255)-(palette[i]&255);
                    int d=dr*dr+dg*dg+db*db;
                    if(d<best) { best=d; index=i; }
                }
                cache[key]=index;
            }
            image.setRGB(x,y,palette[index]);
        }
    }

    static void writePack(Pack p) throws Exception {
        int size=p.actor.size;
        BufferedImage atlas=new BufferedImage(size*6,size*ORDER.length,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=atlas.createGraphics();
        for(int row=0;row<ORDER.length;row++) {
            String name=ORDER[row]; List<BufferedImage> clip=p.clips.get(name);
            for(int col=0;col<clip.size();col++) {
                BufferedImage frame=clip.get(col);
                ImageIO.write(frame,"png",p.directory.resolve(name+"_"+(col+1)+".png").toFile());
                g.drawImage(frame,col*size,row*size,null);
            }
        }
        g.dispose(); ImageIO.write(atlas,"png",p.directory.resolve("sprite_sheet.png").toFile());
        Files.writeString(p.directory.resolve("manifest.json"),manifest(p),StandardCharsets.UTF_8);
        StringBuilder properties=new StringBuilder("version=2\nframeWidth="+size+"\nframeHeight="+size
                +"\npivotX="+p.pivotX+"\npivotY="+p.pivotY+"\nscale="+(p.actor.boss?2:1)
                +"\nanchor="+(p.actor.floating?"center":"feet")+"\noffsetY="+(p.actor.floating?-24:0)+"\n");
        for(String name:ORDER)properties.append(name).append(".secondsPerFrame=")
                .append(String.format(Locale.ROOT,"%.8f",seconds(name,p.actor.id))).append('\n')
                .append(name).append(".loop=").append(LOOPS.contains(name)).append('\n');
        Files.writeString(p.directory.resolve("animation.properties"),properties,StandardCharsets.UTF_8);
        String text="Actor: "+p.actor.label+"\nFrame: "+size+"x"+size+" px\nAtlas: "+size*6+"x"+size*12
                +" px; 6 columns, 12 rows\nPivot: "+p.pivotX+","+p.pivotY+" ("+(p.actor.floating?"reactor centre":"feet")+")\n"
                +"Alpha: binary 0/255; nearest-neighbour; 64-colour shared actor palette\nRow order: "+String.join(", ",ORDER)
                +"\nFirst ten rows: six frames each; fall and land: two source-authored jump poses each\n"
                +"Runtime: assets/"+(p.actor.boss?"bosses/":"players/")+p.actor.id+"/v2/\nBoss guard is visual only.\n";
        Files.writeString(p.directory.resolve("manifest.txt"),text,StandardCharsets.UTF_8);
    }
    static String q(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\""; }
    static String rect(Rectangle r) { return "["+r.x+","+r.y+","+r.width+","+r.height+"]"; }
    static double seconds(String name,String actor) {
        if(name.equals("attack")) return switch(actor) { case "phan_si_kien"->26.0/60/6;case "ronaldo"->18.0/60/6;case "do_mixi"->42.0/60/6;case "the_void_core"->.15/6;default->.30/6; };
        if(name.equals("special")) return actor.equals("the_void_core")?.18/6:actor.equals("the_time_guardian")?.45/6:.54/6;
        return switch(name) { case "idle"->.14;case "walk"->.12;case "run"->.075;case "jump"->.12;
            case "block"->.5/6;case "hurt"->.05;case "death"->.14;case "dash"->.025;default->.10; };
    }
    static String manifest(Pack p) {
        StringBuilder json=new StringBuilder("{\n  \"version\":2,\n  \"status\":\"runtime-ready-v2\",\n  \"actor\":").append(q(p.actor.id));
        json.append(",\n  \"label\":").append(q(p.actor.label)).append(",\n  \"frame_width\":").append(p.actor.size)
                .append(",\n  \"frame_height\":").append(p.actor.size).append(",\n  \"columns\":6,\n  \"rows\":12,\n  \"pivot\":[").append(p.pivotX).append(',').append(p.pivotY).append("]")
                .append(",\n  \"anchor\":").append(q(p.actor.floating?"reactor-centre":"feet"))
                .append(",\n  \"render_offset_y\":").append(p.actor.floating?-24:0)
                .append(",\n  \"recommended_integer_scale\":").append(p.actor.boss?2:1)
                .append(",\n  \"resampling\":\"nearest-neighbour\",\n  \"alpha_cutoff\":128,\n  \"palette\":[");
        for(int i=0;i<p.palette.length;i++) { if(i>0)json.append(',');json.append(q(String.format("#%06x",p.palette[i]&0xffffff))); }
        json.append("],\n  \"animations\":{\n");
        for(int row=0;row<ORDER.length;row++) {
            String name=ORDER[row]; int count=p.clips.get(name).size();
            if(row>0)json.append(",\n");
            json.append("    ").append(q(name)).append(":{\"row\":").append(row).append(",\"frames\":").append(count)
                    .append(",\"loop\":").append(LOOPS.contains(name)).append(",\"hold_last\":").append(name.equals("death"))
                    .append(",\"seconds_per_frame\":").append(String.format(Locale.ROOT,"%.8f",seconds(name,p.actor.id)))
                    .append(",\"rects\":[");
            for(int col=0;col<count;col++) { if(col>0)json.append(',');json.append(rect(new Rectangle(col*p.actor.size,row*p.actor.size,p.actor.size,p.actor.size))); }
            json.append("]");
            if(name.equals("fall"))json.append(",\"derived_from\":\"jump_3.png, jump_4.png\"");
            if(name.equals("land"))json.append(",\"derived_from\":\"jump_5.png, jump_6.png\"");
            if(name.equals("attack"))json.append(",\"phase_frames\":{\"startup\":[0,1],\"active\":[2,3],\"recovery\":[4,5]}");
            json.append('}');
        }
        json.append("\n  },\n  \"source_path_base\":\"project-root\",\n  \"sources\":[");
        int sourceIndex=0;
        for(Sheet sheet:List.of(p.move,p.combat)) {
            if(sourceIndex++>0)json.append(',');
            json.append("\n    {\"file\":").append(q("art/animations/v2/source/"+p.actor.id+"-"+sheet.name+".png"))
                    .append(",\"width\":").append(sheet.image.getWidth()).append(",\"height\":").append(sheet.image.getHeight())
                    .append(",\"scale\":").append(String.format(Locale.ROOT,"%.8f",sheet.scale)).append(",\"cuts_y\":").append(Arrays.toString(sheet.rows))
                    .append(",\"cells\":[");
            int index=0;
            for(Cell[] row:sheet.cells)for(Cell cell:row) {
                if(index++>0)json.append(',');json.append("{\"rect\":").append(rect(cell.source))
                        .append(",\"source_root\":[").append(String.format(Locale.ROOT,"%.3f,%.3f",cell.rootX,cell.rootY)).append("]")
                        .append(",\"source_scale\":").append(String.format(Locale.ROOT,"%.8f",cell.sourceScale));
                if(cell.overrideFile!=null)json.append(",\"override_file\":").append(q(cell.overrideFile));
                json.append('}');
            }
            json.append("]}");
        }
        return json.append("\n  ]\n}\n").toString();
    }
    static long hash(BufferedImage image) {
        long h=0xcbf29ce484222325L;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) h=(h^image.getRGB(x,y))*0x100000001b3L;
        return h;
    }
    static void validate(Pack p) {
        for(String name:ORDER) {
            List<BufferedImage> clip=p.clips.get(name);Set<Long> unique=new HashSet<>();
            require(clip.size()==(name.equals("fall")||name.equals("land")?2:6),"Wrong frame count");
            for(BufferedImage frame:clip) {
                require(frame.getWidth()==p.actor.size&&frame.getHeight()==p.actor.size,"Inconsistent dimensions");
                Rectangle visible=bounds(frame,false);require(visible!=null,"Empty output frame");
                require(visible.x>0&&visible.y>0&&visible.getMaxX()<p.actor.size&&visible.getMaxY()<p.actor.size,"Output edge clipped: "+p.actor.id+"/"+name);
                for(int y=0;y<frame.getHeight();y++)for(int x=0;x<frame.getWidth();x++) {
                    int alpha=frame.getRGB(x,y)>>>24;
                    if(alpha!=0&&alpha!=255)throw new IllegalStateException("Non-binary alpha");
                }
                unique.add(hash(frame));
            }
            require(unique.size()>=2,"No actual pose changes in "+p.actor.id+"/"+name);
        }
        double idle=bounds(p.clips.get("idle").get(0),false).height;
        double death=bounds(p.clips.get("death").get(5),false).height;
        require(death<idle*.72,"Death endpoint not defeated/low: "+p.actor.id);
        require(hash(p.clips.get("walk").get(2))!=hash(p.clips.get("run").get(2)),"Walk reused for run");
        require(hash(p.clips.get("attack").get(3))!=hash(p.clips.get("special").get(3)),"Attack reused for skill");
        System.out.printf(Locale.ROOT,"%s: 64 frames, 12 states, %dx%d, death/idle height %.2f, shared palette %d PASS%n",
                p.actor.id,p.actor.size,p.actor.size,death/idle,p.palette.length);
    }

    static final class Gif implements AutoCloseable {
        final ImageWriter writer=ImageIO.getImageWritersBySuffix("gif").next();
        final ImageOutputStream output;
        boolean first=true;
        Gif(Path path)throws Exception { output=ImageIO.createImageOutputStream(path.toFile());writer.setOutput(output);writer.prepareWriteSequence(null); }
        void add(BufferedImage image,int milliseconds)throws Exception {
            ImageWriteParam params=writer.getDefaultWriteParam();
            IIOMetadata metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),params);
            String format=metadata.getNativeMetadataFormatName();
            IIOMetadataNode root=(IIOMetadataNode)metadata.getAsTree(format);
            IIOMetadataNode control=node(root,"GraphicControlExtension");
            control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");
            control.setAttribute("transparentColorFlag","FALSE");control.setAttribute("transparentColorIndex","0");
            control.setAttribute("delayTime",Integer.toString(Math.max(1,milliseconds/10)));
            if(first) {
                IIOMetadataNode extension=new IIOMetadataNode("ApplicationExtension");
                extension.setAttribute("applicationID","NETSCAPE");extension.setAttribute("authenticationCode","2.0");
                extension.setUserObject(new byte[]{1,0,0});node(root,"ApplicationExtensions").appendChild(extension);first=false;
            }
            metadata.setFromTree(format,root);writer.writeToSequence(new IIOImage(image,null,metadata),params);
        }
        static IIOMetadataNode node(IIOMetadataNode parent,String name) {
            for(int i=0;i<parent.getLength();i++)if(parent.item(i).getNodeName().equals(name))return (IIOMetadataNode)parent.item(i);
            IIOMetadataNode child=new IIOMetadataNode(name);parent.appendChild(child);return child;
        }
        public void close()throws Exception { writer.endWriteSequence();output.close();writer.dispose(); }
    }
    static BufferedImage preview(List<Pack> packs,String state,int frame) {
        BufferedImage image=new BufferedImage(1536,640,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(12,15,24));g.fillRect(0,0,1536,640);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        PixelFont.drawBaseline(g,"CHRONO RUNNER / "+state.toUpperCase(Locale.ROOT),32,56,3,new Color(220,230,248),1);
        PixelFont.drawBaseline(g,"ANIMATION V2",32,92,2,new Color(130,151,179),1);
        int[] centres={144,368,592,904,1304};
        for(int i=0;i<packs.size();i++) {
            Pack p=packs.get(i);List<BufferedImage> clip=p.clips.get(state);BufferedImage sprite=clip.get(frame%clip.size());
            int scale=p.actor.previewScale;
            int rootY=p.actor.floating?500-(int)Math.round(p.actor.bodyHeight*scale/2.0):500;
            g.drawImage(sprite,centres[i]-p.pivotX*scale,rootY-p.pivotY*scale,sprite.getWidth()*scale,sprite.getHeight()*scale,null);
            int x=centres[i]-PixelFont.width(p.actor.label,2,0)/2;
            PixelFont.drawBaseline(g,p.actor.label,x,558,2,new Color(220,230,248),0);
        }
        PixelFont.drawBaseline(g,"PNG ALPHA / FIXED PIVOT / NEAREST NEIGHBOUR",32,616,2,new Color(130,151,179),0);
        g.dispose();return image;
    }
    static void previews(List<Pack> packs)throws Exception {
        Files.createDirectories(ROOT.resolve("previews"));
        try(Gif all=new Gif(ROOT.resolve("previews/all-animations.gif"))) {
            for(String state:ORDER) {
                int count=packs.get(0).clips.get(state).size();
                try(Gif clip=new Gif(ROOT.resolve("previews/"+state+".gif"))) {
                    for(int index=0;index<count;index++) {
                        BufferedImage image=preview(packs,state,index);
                        if(index==Math.min(3,count-1))ImageIO.write(image,"png",ROOT.resolve("previews/"+state+".png").toFile());
                        clip.add(image,120);
                    }
                }
                int cycles=LOOPS.contains(state)?(count<6?4:2):1;
                for(int cycle=0;cycle<cycles;cycle++)for(int index=0;index<count;index++) {
                    int delay=state.equals("run")?80:120;
                    if(!LOOPS.contains(state)&&index==count-1)delay=state.equals("death")?1200:600;
                    all.add(preview(packs,state,index),delay);
                }
                System.out.println("Animated preview: "+state);
            }
        }
    }
    public static void main(String[] args)throws Exception {
        List<Pack> packs=new ArrayList<>();
        for(Actor actor:ACTORS)packs.add(prepare(actor));
        if(Arrays.asList(args).contains("--install")) for(Pack pack:packs) {
            Path destination=Path.of("assets",pack.actor.boss?"bosses":"players",pack.actor.id,"v2");
            Files.createDirectories(destination);
            try(var files=Files.list(pack.directory)) {
                for(Path file:files.toList())Files.copy(file,destination.resolve(file.getFileName()),StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Installed: "+destination);
        }
        if(!Arrays.asList(args).contains("--no-preview"))previews(packs);
        String summary="PASS: "+checks+" structural checks; 5 actors, 320 PNG frame files, 12 states per actor. Asset bank verified.\n";
        Files.writeString(ROOT.resolve("validation.txt"),summary,StandardCharsets.UTF_8);System.out.print(summary);
    }
}
