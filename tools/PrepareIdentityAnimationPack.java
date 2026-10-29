import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.ImageOutputStream;

/**
 * Technical packaging of externally authored V3 identity animation PNGs.
 * No pose is drawn, invented, duplicated or repaired here. Operations are
 * limited to alpha cleanup, measured crops, uniform nearest-neighbour scale,
 * shared palette reduction, fixed-pivot placement and metadata/previews.
 *
 * Run from project root:
 * java -Djava.awt.headless=true tools/PrepareIdentityAnimationPack.java --install
 * Optional: --actors=ronaldo,do_mixi,phan_si_kien --no-preview --inspect
 * Measured pack-specific crops in metadata/measured-overrides.properties are
 * loaded automatically, followed by source/slicing.properties and any explicit
 * --overrides=path.properties. Source PNGs are never modified.
 *
 * source/slicing.properties may contain these measured, zero-based overrides:
 * ronaldo.combo.cutsY=0,200,400,600,800,1024
 * ronaldo.combo.row.0.cutsX=0,256,512,768,1024,1280,1536
 * ronaldo.combo.referenceHeight=150
 * ronaldo.light_1.1.rect=0,0,256,200
 * ronaldo.light_1.1.source=art/animations/v3/source/authored-replacement.png
 * ronaldo.light_1.1.sourceScale=1.0
 * ronaldo.light_1.1.bodyRect=90,20,65,150
 * ronaldo.heavy.5.excludeRects=105,204,70,15
 * ronaldo.light_1.1.rootX=120
 * ronaldo.light_1.1.rootY=180
 * A replacement source without a rect uses the whole authored PNG.
 */
public final class PrepareIdentityAnimationPack {
    static final Path PROJECT=Path.of("").toAbsolutePath().normalize();
    static final Path ROOT=Path.of("art/animations/v3");
    static final Path SOURCE=ROOT.resolve("source");
    static final int SIZE=128, PIVOT_X=64, PIVOT_Y=116, BODY_HEIGHT=64;
    static final String[] MOVE={"idle","walk","run","jump","dash"};
    static final String[] COMBO={"light_1","light_2","light_3","heavy","launcher"};
    static final String[] REACTION={"air_attack","block","hurt","death","charge"};
    static final String[] SKILLS={"skill_u","skill_i","ultimate"};
    static final String[] ORDER={"idle","run","jump","attack","hurt","death","walk","special","block","dash",
        "fall","land","light_1","light_2","light_3","heavy","launcher","air_attack","charge","skill_u","skill_i","ultimate"};
    static final Set<String> LOOPS=Set.of("idle","walk","run","fall","charge");
    static final Set<String> ALIASES=Set.of("attack","special","fall","land");
    static final Set<String> ACTORS=Set.of("ronaldo","do_mixi","phan_si_kien");
    static final Properties OVERRIDES=new Properties();
    static int checks;

    static final class Cell {
        String state,file; int frame;
        BufferedImage image,output,bodyImage;
        Rectangle source,visible,body,outputBody;
        final List<Rectangle> excludedSourceRects=new ArrayList<>();
        double rootX,rootY,sourceScale=1,scale;
        boolean replacement,imported;
    }
    static final class Sheet {
        String name,file,sha256; String[] states; BufferedImage image;
        int[] rows; int[][] columns; Cell[][] cells;
        double referenceHeight,scale;
    }
    static final class Pack {
        String actor; Path directory;
        final List<Sheet> sheets=new ArrayList<>();
        final Map<String,List<BufferedImage>> clips=new LinkedHashMap<>();
        final Map<String,List<Cell>> cells=new LinkedHashMap<>();
        final List<String> reports=new ArrayList<>();
        int[] palette;
    }
    static void require(boolean test,String message) {
        checks++;
        if(!test)throw new IllegalStateException(message);
    }
    static String relative(Path path) {
        return PROJECT.relativize(path.toAbsolutePath().normalize()).toString().replace('\\','/');
    }
    static String prefix(String actor,String state,int frame) { return actor+"."+state+"."+frame; }
    static BufferedImage clean(Path path) throws Exception {
        require(Files.isRegularFile(path),"Missing authored source: "+relative(path));
        BufferedImage raw=ImageIO.read(path.toFile());
        require(raw!=null,"Cannot decode PNG: "+path);
        require(raw.getColorModel().hasAlpha(),"Source has no alpha channel: "+path);
        BufferedImage image=new BufferedImage(raw.getWidth(),raw.getHeight(),BufferedImage.TYPE_INT_ARGB);
        int transparent=0,opaque=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int value=raw.getRGB(x,y),alpha=value>>>24;
            if(alpha<128)transparent++; else opaque++;
            image.setRGB(x,y,alpha<128?0:0xff000000|(value&0xffffff));
        }
        require(transparent>0&&opaque>0,"Source needs visible art and genuine transparent space: "+path);
        return image;
    }
    /** Identity-neutral heuristic excludes saturated neon VFX, includes light kit and skin. */
    static boolean body(int pixel) {
        if((pixel>>>24)==0)return false;
        int r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
        if(g>120&&b>135&&r<125&&g>r*1.35)return false;
        if(b>165&&r>135&&g<105)return false;
        if(r>210&&g>130&&b<100&&r-g>45)return false;
        if(g>180&&g>r*1.65&&g>b*1.25)return false;
        return true;
    }
    static Rectangle bounds(BufferedImage image,boolean bodyOnly) {
        int x0=image.getWidth(),y0=image.getHeight(),x1=-1,y1=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int pixel=image.getRGB(x,y);
            if(bodyOnly?body(pixel):(pixel>>>24)!=0) {
                x0=Math.min(x0,x);y0=Math.min(y0,y);x1=Math.max(x1,x);y1=Math.max(y1,y);
            }
        }
        return x1<0?null:new Rectangle(x0,y0,x1-x0+1,y1-y0+1);
    }
    static BufferedImage characterMask(BufferedImage image,String actor) {
        int width=image.getWidth(),height=image.getHeight();
        BufferedImage mask=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int pixel=image.getRGB(x,y);boolean character=body(pixel);
            if(actor.equals("phan_si_kien")) {
                int r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
                character&=!(r>70&&r>g*1.45&&r>b*1.15)&&r+g+b<450; // V2 scarf/blade measurement.
            }
            if(character)mask.setRGB(x,y,0xffffffff);
        }
        if(!actor.equals("ronaldo"))return mask;
        // A detached football is visible art, but is not part of the person's
        // physical body. Choose the connected red-kit component solely for
        // geometry measurement; the unmodified visible sprite keeps the ball.
        boolean[] visited=new boolean[width*height];int[] queue=new int[width*height],best=null;
        long bestScore=-1;
        for(int start=0;start<visited.length;start++) {
            if(visited[start]||(mask.getRGB(start%width,start/width)>>>24)==0)continue;
            int read=0,write=1,red=0;queue[0]=start;visited[start]=true;
            while(read<write) {
                int point=queue[read++],x=point%width,y=point/width,pixel=image.getRGB(x,y);
                int r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
                if(r>90&&r>g*1.35&&r>b*1.25)red++;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int xx=x+dx,yy=y+dy;if(xx<0||yy<0||xx>=width||yy>=height)continue;
                    int next=yy*width+xx;if(!visited[next]&&(mask.getRGB(xx,yy)>>>24)!=0){visited[next]=true;queue[write++]=next;}
                }
            }
            long score=write+red*4L;
            if(score>bestScore){bestScore=score;best=Arrays.copyOf(queue,write);}
        }
        mask=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        if(best!=null)for(int point:best)mask.setRGB(point%width,point/width,0xffffffff);
        return mask;
    }
    static void measureComponents(String[] arguments) throws Exception {
        require(arguments.length==6,"Expected --components=actor:sheet:x:y:width:height");
        BufferedImage image=clean(SOURCE.resolve(arguments[0]+"-"+arguments[1]+".png"));
        Rectangle region=new Rectangle(Integer.parseInt(arguments[2]),Integer.parseInt(arguments[3]),Integer.parseInt(arguments[4]),Integer.parseInt(arguments[5]));
        image=crop(image,region,"component measurement");int width=image.getWidth(),height=image.getHeight();
        boolean[] visited=new boolean[width*height];int[] queue=new int[visited.length];
        for(int start=0;start<visited.length;start++) {
            if(visited[start]||(image.getRGB(start%width,start/width)>>>24)==0)continue;
            int read=0,write=1,x0=width,y0=height,x1=-1,y1=-1;queue[0]=start;visited[start]=true;
            while(read<write) {
                int point=queue[read++],x=point%width,y=point/width;x0=Math.min(x0,x);y0=Math.min(y0,y);x1=Math.max(x1,x);y1=Math.max(y1,y);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int xx=x+dx,yy=y+dy;if(xx<0||yy<0||xx>=width||yy>=height)continue;int next=yy*width+xx;
                    if(!visited[next]&&(image.getRGB(xx,yy)>>>24)!=0){visited[next]=true;queue[write++]=next;}
                }
            }
            System.out.println("pixels="+write+" globalRect="+rect(new Rectangle(x0+region.x,y0+region.y,x1-x0+1,y1-y0+1)));
        }
    }
    static BufferedImage crop(BufferedImage image,Rectangle rect,String description) {
        require(rect.width>0&&rect.height>0&&rect.x>=0&&rect.y>=0
            &&rect.getMaxX()<=image.getWidth()&&rect.getMaxY()<=image.getHeight(),"Invalid crop for "+description+": "+rect);
        BufferedImage result=new BufferedImage(rect.width,rect.height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();g.drawImage(image,-rect.x,-rect.y,null);g.dispose();return result;
    }
    static int[] integers(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
    }
    static int[] validatedCuts(String value,int count,int limit,String description) {
        int[] cuts=integers(value);
        require(cuts.length==count+1,"Expected "+(count+1)+" cuts in "+description);
        require(cuts[0]==0&&cuts[count]==limit,"Cuts must start at 0 and end at "+limit+": "+description);
        for(int i=1;i<cuts.length;i++)require(cuts[i]>cuts[i-1],"Non-increasing cuts in "+description);
        return cuts;
    }
    /** Find transparent separators near expected grid lines. Unequal gutters are supported. */
    static int[] cuts(int[] projection,int count,double radius) {
        int[] result=new int[count+1];result[count]=projection.length;
        for(int i=1;i<count;i++) {
            int expected=projection.length*i/count;
            int lo=Math.max(result[i-1]+8,(int)(expected-projection.length*radius));
            int hi=Math.min(projection.length-8,(int)(expected+projection.length*radius));
            long best=Long.MAX_VALUE;int selected=expected;
            for(int value=lo;value<=hi;value++) {
                long occupancy=0;
                for(int k=-2;k<=2;k++)occupancy+=projection[Math.max(0,Math.min(projection.length-1,value+k))];
                long score=occupancy*100000L+Math.abs(value-expected);
                if(score<best){best=score;selected=value;}
            }
            result[i]=selected;
        }
        return result;
    }
    static double median(double[] values) {
        double[] copy=values.clone();Arrays.sort(copy);return copy[copy.length/2];
    }
    static Sheet readSheet(String actor,String name,String[] states) throws Exception {
        Sheet sheet=new Sheet();sheet.name=name;sheet.states=states;
        Path path=SOURCE.resolve(actor+"-"+name+".png");sheet.file=relative(path);sheet.image=clean(path);
        sheet.sha256=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        int w=sheet.image.getWidth(),h=sheet.image.getHeight(),rowCount=states.length;
        int[] vertical=new int[h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((sheet.image.getRGB(x,y)>>>24)!=0)vertical[y]++;
        String key=actor+"."+name+".cutsY",override=OVERRIDES.getProperty(key);
        sheet.rows=override==null?cuts(vertical,rowCount,.10):validatedCuts(override,rowCount,h,key);
        System.out.println("Measured "+sheet.file+" "+w+"x"+h+" cutsY="+Arrays.toString(sheet.rows));
        sheet.columns=new int[rowCount][];sheet.cells=new Cell[rowCount][6];
        for(int row=0;row<rowCount;row++) {
            int[] horizontal=new int[w];
            for(int y=sheet.rows[row];y<sheet.rows[row+1];y++)for(int x=0;x<w;x++) {
                int pixel=sheet.image.getRGB(x,y);if((pixel>>>24)!=0)horizontal[x]+=body(pixel)?9:1;
            }
            key=actor+"."+name+".row."+row+".cutsX";override=OVERRIDES.getProperty(key);
            sheet.columns[row]=override==null?cuts(horizontal,6,.045):validatedCuts(override,6,w,key);
        }
        sourceGuide(actor,sheet);
        for(int row=0;row<rowCount;row++) {
            if(actor.equals("phan_si_kien")&&name.equals("reaction")&&row>0&&row<4)continue;
            for(int col=0;col<6;col++) {
                Cell cell=new Cell();cell.state=states[row];cell.frame=col+1;cell.file=sheet.file;
                cell.source=new Rectangle(sheet.columns[row][col],sheet.rows[row],
                    sheet.columns[row][col+1]-sheet.columns[row][col],sheet.rows[row+1]-sheet.rows[row]);
                BufferedImage origin=sheet.image;String frameKey=prefix(actor,cell.state,cell.frame);
                String extra=OVERRIDES.getProperty(frameKey+".source");
                if(extra!=null) {
                    Path replacement=PROJECT.resolve(extra).normalize();
                    require(replacement.startsWith(PROJECT),"Replacement must be inside project: "+extra);
                    origin=clean(replacement);cell.file=relative(replacement);cell.replacement=true;
                    cell.source=new Rectangle(0,0,origin.getWidth(),origin.getHeight());
                }
                override=OVERRIDES.getProperty(frameKey+".rect");
                if(override!=null) {
                    int[] rect=integers(override);require(rect.length==4,"Expected x,y,width,height: "+frameKey+".rect");
                    cell.source=new Rectangle(rect[0],rect[1],rect[2],rect[3]);
                }
                cell.image=crop(origin,cell.source,frameKey);
                String exclusions=OVERRIDES.getProperty(frameKey+".excludeRects");
                if(exclusions!=null)for(String entry:exclusions.split(";")) {
                    int[] values=integers(entry);require(values.length==4,"Expected local x,y,width,height: "+frameKey+".excludeRects");
                    Rectangle exclude=new Rectangle(values[0],values[1],values[2],values[3]);
                    require(exclude.x>=0&&exclude.y>=0&&exclude.width>0&&exclude.height>0
                        &&exclude.getMaxX()<=cell.image.getWidth()&&exclude.getMaxY()<=cell.image.getHeight(),"Invalid exclusion crop: "+frameKey);
                    // Explicit measured crop of a neighbouring atlas cell's
                    // art. No original pixel is changed, drawn or recoloured.
                    for(int yy=exclude.y;yy<exclude.getMaxY();yy++)for(int xx=exclude.x;xx<exclude.getMaxX();xx++)cell.image.setRGB(xx,yy,0);
                    cell.excludedSourceRects.add(exclude);
                }
                cell.visible=bounds(cell.image,false);
                cell.bodyImage=characterMask(cell.image,actor);
                String bodyRect=OVERRIDES.getProperty(frameKey+".bodyRect");
                if(bodyRect!=null) {
                    int[] values=integers(bodyRect);require(values.length==4,"Expected x,y,width,height: "+frameKey+".bodyRect");
                    Rectangle restrict=new Rectangle(values[0],values[1],values[2],values[3]);
                    for(int yy=0;yy<cell.bodyImage.getHeight();yy++)for(int xx=0;xx<cell.bodyImage.getWidth();xx++)
                        if(!restrict.contains(xx,yy))cell.bodyImage.setRGB(xx,yy,0);
                }
                cell.body=bounds(cell.bodyImage,false);
                require(cell.visible!=null,"Empty authored frame "+frameKey+" at "+cell.source+" in "+cell.file
                    +"; correct source/slicing.properties or supply an authored replacement PNG. No frame is duplicated.");
                require(cell.body!=null&&cell.body.height>5,"Missing character body in "+frameKey+" at "+cell.source+" in "+cell.file);
                cell.sourceScale=Double.parseDouble(OVERRIDES.getProperty(frameKey+".sourceScale","1"));
                require(Double.isFinite(cell.sourceScale)&&cell.sourceScale>0,"Invalid sourceScale: "+frameKey);
                sheet.cells[row][col]=cell;
                // A cut crossing opaque pixels can merge or lose an adjacent pose.
                // Manual rects remain subject to this check; authored single PNGs
                // require transparent margins too, so cropping never hides damage.
                require(cell.visible.x>0&&cell.visible.y>0&&cell.visible.getMaxX()<cell.image.getWidth()
                    &&cell.visible.getMaxY()<cell.image.getHeight(),"Crop intersects opaque art in "+frameKey+" from "+cell.file
                    +" (rect="+cell.source+", local visible="+cell.visible+"). Measure cuts/rects or replace authored source.");
            }
        }
        // Each file is calibrated using its returned standing/recovery poses.
        // Prone death endpoints and airborne attack rows are excluded.
        List<Double> heights=new ArrayList<>();
        if(name.equals("locomotion"))for(Cell cell:sheet.cells[0])heights.add(cell.body.height*cell.sourceScale);
        else for(int row=0;row<states.length;row++)if(sheet.cells[row][5]!=null&&!states[row].equals("death")&&!states[row].equals("air_attack"))
            heights.add(sheet.cells[row][5].body.height*sheet.cells[row][5].sourceScale);
        sheet.referenceHeight=Double.parseDouble(OVERRIDES.getProperty(actor+"."+name+".referenceHeight",
            Double.toString(median(heights.stream().mapToDouble(Double::doubleValue).toArray()))));
        require(sheet.referenceHeight>5,"Invalid reference height for "+sheet.file);
        sheet.scale=BODY_HEIGHT/sheet.referenceHeight;
        return sheet;
    }
    static void sourceGuide(String actor,Sheet sheet) throws Exception {
        Path directory=ROOT.resolve("previews/source-cuts");Files.createDirectories(directory);
        BufferedImage guide=new BufferedImage(sheet.image.getWidth(),sheet.image.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=guide.createGraphics();background(g,guide.getWidth(),guide.getHeight());g.drawImage(sheet.image,0,0,null);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
        for(int row=0;row<sheet.states.length;row++)for(int col=0;col<6;col++) {
            int x=sheet.columns[row][col],y=sheet.rows[row],width=sheet.columns[row][col+1]-x,height=sheet.rows[row+1]-y;
            g.setColor(new Color(240,100,180));g.drawRect(x,y,width-1,height-1);
            g.setColor(new Color(255,255,255));g.drawString(sheet.states[row]+"_"+(col+1),x+5,y+17);
        }
        g.dispose();ImageIO.write(guide,"png",directory.resolve(actor+"-"+sheet.name+".png").toFile());
    }
    static void inspectSheet(String actor,Sheet sheet) throws Exception {
        StringBuilder report=new StringBuilder(actor+"/"+sheet.name+" "+sheet.image.getWidth()+"x"+sheet.image.getHeight()
            +" cutsY="+Arrays.toString(sheet.rows)+" bodyRef="+format(sheet.referenceHeight)+"\n");
        for(int row=0;row<sheet.states.length;row++) {
            report.append("  ").append(sheet.states[row]).append(" cutsX=").append(Arrays.toString(sheet.columns[row]));
            if(sheet.cells[row][0]==null)report.append(" SKIPPED: retained V2 state\n");
            else report.append(" heights=").append(Arrays.toString(Arrays.stream(sheet.cells[row]).mapToInt(c->c.body.height).toArray())).append('\n');
        }
        System.out.print(report);Files.createDirectories(ROOT.resolve("metadata"));
        Files.writeString(ROOT.resolve("metadata/"+actor+"-"+sheet.name+"-slicing.txt"),report,StandardCharsets.UTF_8);
        Pack partial=new Pack();partial.actor=actor;
        for(int row=0;row<sheet.states.length;row++)for(Cell cell:sheet.cells[row]) {
            if(cell==null)continue;
            String key=prefix(actor,cell.state,cell.frame);double scale=sheet.scale*cell.sourceScale;
            double measured=Double.parseDouble(OVERRIDES.getProperty(key+".rootX",Double.toString(footX(cell,sheet.referenceHeight,actor))));
            double min=cell.visible.getMaxX()-(SIZE-PIVOT_X-2)/scale,max=cell.visible.x+(PIVOT_X-2)/scale;
            if(measured<min||measured>max)System.out.println("Measured-pivot candidate "+key+".rootX="+format(Math.max(min,Math.min(max,measured)))
                +" (output shift="+format((Math.max(min,Math.min(max,measured))-measured)*scale)+"px; source body="+rect(cell.body)+"; feasible="+(min<=max)+")");
        }
        renderSheet(partial,sheet);
        for(String state:partial.clips.keySet()) {
            Set<Long> unique=new HashSet<>();for(BufferedImage image:partial.clips.get(state))unique.add(hash(image));
            require(unique.size()==6,"Need six distinct poses in inspected source: "+actor+"/"+state);
            String result=state+": 6 unique; fits128x128 at64px calibration; output body heights="
                +partial.cells.get(state).stream().map(c->Integer.toString(c.outputBody.height)).toList()+"\n";
            System.out.print(result);report.append(result);
        }
        Files.writeString(ROOT.resolve("metadata/"+actor+"-"+sheet.name+"-slicing.txt"),report,StandardCharsets.UTF_8);
        contactSheet(partial,new ArrayList<>(partial.clips.keySet()),ROOT.resolve("previews/"+actor+"-"+sheet.name+"-normalized.png"));
    }
    static double footX(Cell cell,double reference,String actor) {
        if(actor.equals("ronaldo")) {
            // Follow the red jersey centre even when the football touches a
            // boot and shares its alpha component. A moving ball must never
            // pull the player's runtime pivot sideways.
            int top=cell.body.y+(int)(cell.body.height*.20),bottom=cell.body.y+(int)(cell.body.height*.58);
            long redX=0,redCount=0;
            for(int y=top;y<bottom;y++)for(int x=0;x<cell.image.getWidth();x++) {
                int pixel=cell.image.getRGB(x,y),r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
                if((cell.bodyImage.getRGB(x,y)>>>24)!=0&&r>90&&r>g*1.35&&r>b*1.25){redX+=x;redCount++;}
            }
            if(redCount>0)return(double)redX/redCount;
        }
        int bottom=cell.body.y+cell.body.height,top=Math.max(cell.body.y,bottom-(int)Math.ceil(reference*.15));
        long sum=0,count=0;
        for(int y=top;y<bottom;y++)for(int x=0;x<cell.image.getWidth();x++)if((cell.bodyImage.getRGB(x,y)>>>24)!=0){sum+=x;count++;}
        return count==0?cell.body.getCenterX():(double)sum/count;
    }
    static void renderSheet(Pack pack,Sheet sheet) {
        for(int row=0;row<sheet.states.length;row++) {
            if(pack.actor.equals("phan_si_kien")&&sheet.name.equals("reaction")
                &&!sheet.states[row].equals("air_attack")&&!sheet.states[row].equals("charge"))continue;
            List<BufferedImage> clip=new ArrayList<>();List<Cell> cells=new ArrayList<>();
            for(Cell cell:sheet.cells[row]) {
                String key=prefix(pack.actor,cell.state,cell.frame);
                cell.rootX=Double.parseDouble(OVERRIDES.getProperty(key+".rootX",Double.toString(footX(cell,sheet.referenceHeight,pack.actor))));
                cell.rootY=Double.parseDouble(OVERRIDES.getProperty(key+".rootY",Double.toString(cell.body.getMaxY())));
                cell.scale=sheet.scale*cell.sourceScale;
                double left=(cell.rootX-cell.visible.x)*cell.scale,right=(cell.visible.getMaxX()-cell.rootX)*cell.scale;
                double above=(cell.rootY-cell.visible.y)*cell.scale,below=(cell.visible.getMaxY()-cell.rootY)*cell.scale;
                require(left<=PIVOT_X-2&&right<=SIZE-PIVOT_X-2&&above<=PIVOT_Y-2&&below<=SIZE-PIVOT_Y-2,
                    "Authored frame exceeds 128px fixed-pivot canvas at 64px body: "+key+" extents L/R/U/D="
                    +format(left)+"/"+format(right)+"/"+format(above)+"/"+format(below)
                    +"; provide measured root/sourceScale or a source with compact effects. No opaque pixels are discarded.");
                BufferedImage output=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=output.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                AffineTransform transform=new AffineTransform();
                transform.translate(PIVOT_X-cell.rootX*cell.scale,PIVOT_Y-cell.rootY*cell.scale);transform.scale(cell.scale,cell.scale);
                g.drawImage(cell.image,transform,null);g.dispose();
                BufferedImage outputMask=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);
                g=outputMask.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(cell.bodyImage,transform,null);g.dispose();cell.outputBody=bounds(outputMask,false);
                cell.output=output;clip.add(output);cells.add(cell);
            }
            pack.clips.put(sheet.states[row],clip);pack.cells.put(sheet.states[row],cells);
        }
    }
    static void importBase(Pack pack) throws Exception {
        Path base=Path.of("assets/players",pack.actor,"v2");
        for(String state:List.of("idle","walk","run","jump","dash","air_attack","block","hurt","death","charge")) {
            String original=state;
            if(state.equals("air_attack")||state.equals("charge"))continue; // Required new source states are handled below.
            List<BufferedImage> images=new ArrayList<>();List<Cell> cells=new ArrayList<>();
            for(int frame=1;frame<=6;frame++) {
                Path path=base.resolve(original+"_"+frame+".png");BufferedImage image=clean(path);
                require(image.getWidth()==SIZE&&image.getHeight()==SIZE,"V2 base dimensions differ: "+path);
                Cell cell=new Cell();cell.state=state;cell.frame=frame;cell.file=relative(path);cell.imported=true;
                cell.image=image;cell.output=image;cell.visible=bounds(image,false);cell.bodyImage=characterMask(image,pack.actor);
                cell.body=bounds(cell.bodyImage,false);cell.outputBody=cell.body;
                cell.source=new Rectangle(0,0,SIZE,SIZE);cell.rootX=PIVOT_X;cell.rootY=PIVOT_Y;cell.scale=1;
                images.add(image);cells.add(cell);
            }
            pack.clips.put(state,images);pack.cells.put(state,cells);
        }
    }
    static Pack prepare(String actor,boolean inspectOnly) throws Exception {
        Pack pack=new Pack();pack.actor=actor;pack.directory=ROOT.resolve("players/"+actor);
        if(actor.equals("phan_si_kien")) {
            importBase(pack);
            pack.sheets.add(readSheet(actor,"combo",COMBO));
            pack.sheets.add(readSheet(actor,"skills",SKILLS));
            // Phan's retained V2 base has no air attack or charge; author them
            // as optional extra sources rather than relabel an old pose.
            Path reactions=SOURCE.resolve(actor+"-reaction.png");
            if(Files.isRegularFile(reactions))pack.sheets.add(readSheet(actor,"reaction",REACTION));
        } else {
            pack.sheets.add(readSheet(actor,"locomotion",MOVE));pack.sheets.add(readSheet(actor,"combo",COMBO));
            pack.sheets.add(readSheet(actor,"reaction",REACTION));pack.sheets.add(readSheet(actor,"skills",SKILLS));
        }
        if(inspectOnly) {
            for(Sheet sheet:pack.sheets)inspectSheet(actor,sheet);
            return pack;
        }
        for(Sheet sheet:pack.sheets)renderSheet(pack,sheet);
        // Retained base frames remain byte-identical PNGs when exported.
        List<BufferedImage> newArt=new ArrayList<>();
        for(var entry:pack.cells.entrySet())for(Cell cell:entry.getValue())if(!cell.imported)newArt.add(cell.output);
        pack.palette=buildPalette(newArt,64);
        for(var entry:pack.cells.entrySet())for(Cell cell:entry.getValue())if(!cell.imported)applyPalette(cell.output,pack.palette);
        alias(pack,"attack","light_1",0,6);alias(pack,"special","skill_u",0,6);
        alias(pack,"fall","jump",2,4);alias(pack,"land","jump",4,6);
        validate(pack);writePack(pack);return pack;
    }
    static void alias(Pack pack,String name,String from,int start,int end) {
        pack.clips.put(name,new ArrayList<>(pack.clips.get(from).subList(start,end)));
        pack.cells.put(name,new ArrayList<>(pack.cells.get(from).subList(start,end)));
    }
    static int key(int rgb){return((rgb>>19)&31)<<10|((rgb>>11)&31)<<5|((rgb>>3)&31);}
    static int channel(int bin,int index){return(bin>>(10-index*5))&31;}
    static final class ColourBox {
        List<Integer> bins;long weight;int splitChannel,range;
        ColourBox(List<Integer> bins,int[] histogram) {
            this.bins=bins;int[] min={31,31,31},max={0,0,0};
            for(int bin:bins){weight+=histogram[bin];for(int c=0;c<3;c++){int v=channel(bin,c);min[c]=Math.min(min[c],v);max[c]=Math.max(max[c],v);}}
            for(int c=0;c<3;c++)if(max[c]-min[c]>range){range=max[c]-min[c];splitChannel=c;}
        }
    }
    static int[] buildPalette(List<BufferedImage> images,int count) {
        int[] histogram=new int[32768];
        for(BufferedImage image:images)for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int rgb=image.getRGB(x,y);if((rgb>>>24)!=0)histogram[key(rgb)]++;
        }
        List<Integer> used=new ArrayList<>();for(int i=0;i<histogram.length;i++)if(histogram[i]>0)used.add(i);
        require(!used.isEmpty(),"No authored colours available");
        List<ColourBox> boxes=new ArrayList<>();boxes.add(new ColourBox(used,histogram));
        while(boxes.size()<count) {
            ColourBox box=boxes.stream().filter(b->b.bins.size()>1).max(Comparator.comparingDouble(b->b.range*Math.sqrt(b.weight))).orElse(null);
            if(box==null)break;int c=box.splitChannel;box.bins.sort(Comparator.comparingInt(bin->channel(bin,c)));
            long sum=0;int split=1;for(int i=0;i<box.bins.size()-1;i++){sum+=histogram[box.bins.get(i)];split=i+1;if(sum>=box.weight/2)break;}
            boxes.remove(box);boxes.add(new ColourBox(new ArrayList<>(box.bins.subList(0,split)),histogram));
            boxes.add(new ColourBox(new ArrayList<>(box.bins.subList(split,box.bins.size())),histogram));
        }
        int[] palette=new int[boxes.size()];
        for(int i=0;i<boxes.size();i++) {
            ColourBox box=boxes.get(i);long r=0,g=0,b=0;
            for(int bin:box.bins){long n=histogram[bin];r+=(channel(bin,0)*8+4)*n;g+=(channel(bin,1)*8+4)*n;b+=(channel(bin,2)*8+4)*n;}
            palette[i]=0xff000000|((int)(r/box.weight)<<16)|((int)(g/box.weight)<<8)|(int)(b/box.weight);
        }
        return palette;
    }
    static void applyPalette(BufferedImage image,int[] palette) {
        int[] cache=new int[32768];Arrays.fill(cache,-1);
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int rgb=image.getRGB(x,y);if((rgb>>>24)==0)continue;int key=key(rgb),index=cache[key];
            if(index<0) {
                int best=Integer.MAX_VALUE;index=0;
                for(int i=0;i<palette.length;i++) {
                    int r=((rgb>>16)&255)-((palette[i]>>16)&255),g=((rgb>>8)&255)-((palette[i]>>8)&255),b=(rgb&255)-(palette[i]&255);
                    int distance=r*r+g*g+b*b;if(distance<best){best=distance;index=i;}
                }
                cache[key]=index;
            }
            image.setRGB(x,y,palette[index]);
        }
    }
    static long hash(BufferedImage image) {
        long value=0xcbf29ce484222325L;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)value=(value^image.getRGB(x,y))*0x100000001b3L;
        return value;
    }
    static List<String> states(Pack pack) { return Arrays.stream(ORDER).filter(pack.clips::containsKey).toList(); }
    static void validate(Pack pack) {
        for(String state:states(pack)) {
            List<BufferedImage> clip=pack.clips.get(state);int expected=state.equals("fall")||state.equals("land")?2:6;
            require(clip.size()==expected,"Wrong frame count in "+pack.actor+"/"+state);
            Set<Long> unique=new HashSet<>();List<String> boxes=new ArrayList<>();
            for(int index=0;index<clip.size();index++) {
                BufferedImage image=clip.get(index);Rectangle visible=bounds(image,false);
                require(image.getWidth()==SIZE&&image.getHeight()==SIZE,"Output dimensions: "+state);
                require(visible!=null,"Empty output: "+state);
                require(visible.x>0&&visible.y>0&&visible.getMaxX()<SIZE&&visible.getMaxY()<SIZE,
                    "Output edge clipping in "+pack.actor+"/"+state+"/"+(index+1)+": "+visible);
                for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++) {
                    int alpha=image.getRGB(x,y)>>>24;require(alpha==0||alpha==255,"Non-binary alpha in "+state);
                }
                unique.add(hash(image));boxes.add(rect(visible));
            }
            require(unique.size()==expected,"Expected "+expected+" distinct authored frames in "+pack.actor+"/"+state+"; found "+unique.size());
            String report=pack.actor+"/"+state+": "+clip.size()+" frames, "+unique.size()+" unique, alpha 0/255, no edge crop; visible="+boxes;
            pack.reports.add(report);System.out.println(report);
        }
        double idle=pack.cells.get("idle").get(0).outputBody.height;
        double death=pack.cells.get("death").get(5).outputBody.height;
        require(death<idle*.72,"Death does not end prone/low: "+pack.actor+" death/idle="+format(death/idle));
        if(!pack.actor.equals("phan_si_kien"))require(idle>=59&&idle<=69,"Standing body needs about 64px: "+pack.actor+" idle="+idle);
        require(hash(pack.clips.get("walk").get(2))!=hash(pack.clips.get("run").get(2)),"Walk reused for run: "+pack.actor);
        require(hash(pack.clips.get("light_1").get(3))!=hash(pack.clips.get("skill_u").get(3)),"Attack reused for skill: "+pack.actor);
        pack.reports.add("Standing body="+idle+"px; death endpoint="+death+"px; death/idle="+format(death/idle)+"; PASS");
    }
    static double seconds(String state,String actor) {
        return switch(state) {
            case "idle"->.14;case "walk"->.12;case "run"->.075;case "jump"->.12;
            case "dash"->(actor.equals("ronaldo")?.13:actor.equals("do_mixi")?.18:.15)/6;
            case "attack","light_1","light_2","light_3","air_attack"->(actor.equals("ronaldo")?18.0:actor.equals("do_mixi")?42.0:26.0)/60/6;
            case "heavy"->40.0/60/6;case "launcher"->37.0/60/6;
            case "block"->.08333333;case "hurt"->.05;case "death"->.14;case "charge"->.1;
            case "special","skill_u"->24.0/60/6;case "skill_i"->45.0/60/6;case "ultimate"->66.0/60/6;default->.10;
        };
    }
    static void writePack(Pack pack) throws Exception {
        Files.createDirectories(pack.directory);List<String> states=states(pack);
        removeStaleFrames(pack.directory,pack);
        BufferedImage atlas=new BufferedImage(SIZE*6,SIZE*states.size(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=atlas.createGraphics();
        for(int row=0;row<states.size();row++) {
            String state=states.get(row);List<BufferedImage> clip=pack.clips.get(state);
            for(int col=0;col<clip.size();col++) {
                Path target=pack.directory.resolve(state+"_"+(col+1)+".png");Cell cell=pack.cells.get(state).get(col);
                if(cell.imported)Files.copy(PROJECT.resolve(cell.file),target,StandardCopyOption.REPLACE_EXISTING);
                else ImageIO.write(clip.get(col),"png",target.toFile());
                g.drawImage(clip.get(col),col*SIZE,row*SIZE,null);
            }
        }
        g.dispose();ImageIO.write(atlas,"png",pack.directory.resolve("sprite_sheet.png").toFile());
        StringBuilder properties=new StringBuilder("version=3\nframeWidth=128\nframeHeight=128\npivotX=64\npivotY=116\nscale=1\nanchor=feet\noffsetY=0\n");
        for(String state:states)properties.append(state).append(".secondsPerFrame=").append(format(seconds(state,pack.actor))).append('\n')
            .append(state).append(".loop=").append(LOOPS.contains(state)).append('\n');
        Files.writeString(pack.directory.resolve("animation.properties"),properties,StandardCharsets.UTF_8);
        Files.writeString(pack.directory.resolve("manifest.json"),manifest(pack),StandardCharsets.UTF_8);
        Files.writeString(pack.directory.resolve("validation.txt"),String.join("\n",pack.reports)+"\n",StandardCharsets.UTF_8);
        String description="V3 identity animation bank: "+pack.actor+"\n128x128px; pivot 64,116; feet; scale 1; alpha 0/255; nearest-neighbour.\n"
            +"Authored PNGs only. Shared 64-colour palette for new art. Phan retained V2 frames are unchanged.\n"
            +"Row order: "+String.join(", ",states)+"\nAliases: attack=light_1; special=skill_u; fall=jump_3,4; land=jump_5,6.\n"
            +"Source coordinates, source dimensions, per-frame visible/body bounds and measured roots are in manifest.json.\n";
        Files.writeString(pack.directory.resolve("manifest.txt"),description,StandardCharsets.UTF_8);
    }
    static String q(String text) { return '"'+text.replace("\\","\\\\").replace("\"","\\\"")+ '"'; }
    static String rect(Rectangle rect) { return "["+rect.x+","+rect.y+","+rect.width+","+rect.height+"]"; }
    static String format(double value) { return String.format(Locale.ROOT,"%.8f",value); }
    static String manifest(Pack pack) {
        List<String> states=states(pack);
        StringBuilder out=new StringBuilder("{\n  \"version\":3,\n  \"status\":\"runtime-ready-v3\",\n  \"actor\":").append(q(pack.actor))
            .append(",\n  \"frame_width\":128,\n  \"frame_height\":128,\n  \"columns\":6,\n  \"rows\":").append(states.size())
            .append(",\n  \"pivot\":[64,116],\n  \"anchor\":\"feet\",\n  \"recommended_integer_scale\":1,\n  \"standing_body_target_px\":64,")
            .append("\n  \"resampling\":\"nearest-neighbour\",\n  \"alpha_cutoff\":128,\n  \"source_path_base\":\"project-root\",\n  \"palette\":[");
        for(int i=0;i<pack.palette.length;i++){if(i>0)out.append(',');out.append(q(String.format("#%06x",pack.palette[i]&0xffffff)));}
        out.append("],\n  \"sources\":[");
        for(int i=0;i<pack.sheets.size();i++) {
            Sheet sheet=pack.sheets.get(i);if(i>0)out.append(',');
            out.append("\n    {\"file\":").append(q(sheet.file)).append(",\"absolute_file\":").append(q(PROJECT.resolve(sheet.file).toString()))
                .append(",\"sha256\":").append(q(sheet.sha256))
                .append(",\"width\":").append(sheet.image.getWidth()).append(",\"height\":").append(sheet.image.getHeight())
                .append(",\"reference_body_height\":").append(format(sheet.referenceHeight)).append(",\"scale\":").append(format(sheet.scale))
                .append(",\"cuts_y\":").append(Arrays.toString(sheet.rows)).append(",\"cuts_x_by_row\":[");
            for(int row=0;row<sheet.columns.length;row++){if(row>0)out.append(',');out.append(Arrays.toString(sheet.columns[row]));}
            out.append("]}");
        }
        out.append("\n  ],\n  \"animations\":{\n");
        for(int row=0;row<states.size();row++) {
            String state=states.get(row);if(row>0)out.append(",\n");
            out.append("    ").append(q(state)).append(":{\"row\":").append(row).append(",\"frames\":").append(pack.clips.get(state).size())
                .append(",\"loop\":").append(LOOPS.contains(state)).append(",\"hold_last\":").append(state.equals("death"))
                .append(",\"seconds_per_frame\":").append(format(seconds(state,pack.actor)));
            if(ALIASES.contains(state))out.append(",\"derived_from\":").append(q(switch(state){case "attack"->"light_1";case "special"->"skill_u";case "fall"->"jump_3,jump_4";default->"jump_5,jump_6";}));
            out.append(",\"cells\":[");List<Cell> cells=pack.cells.get(state);
            for(int col=0;col<cells.size();col++) {
                Cell cell=cells.get(col);if(col>0)out.append(',');
                out.append("{\"atlas_rect\":").append(rect(new Rectangle(col*SIZE,row*SIZE,SIZE,SIZE)))
                    .append(",\"source_file\":").append(q(cell.file)).append(",\"source_rect\":").append(rect(cell.source))
                    .append(",\"source_absolute_file\":").append(q(PROJECT.resolve(cell.file).toString()))
                    .append(",\"source_visible_bounds\":").append(rect(cell.visible)).append(",\"source_body_bounds\":").append(rect(cell.body))
                    .append(",\"source_visible_bounds_global\":").append(rect(new Rectangle(cell.visible.x+cell.source.x,cell.visible.y+cell.source.y,cell.visible.width,cell.visible.height)))
                    .append(",\"excluded_foreign_cell_rects\":[");
                for(int excluded=0;excluded<cell.excludedSourceRects.size();excluded++){if(excluded>0)out.append(',');out.append(rect(cell.excludedSourceRects.get(excluded)));}
                out.append(']')
                    .append(",\"source_root\":[").append(format(cell.rootX)).append(',').append(format(cell.rootY)).append(']')
                    .append(",\"scale\":").append(format(cell.scale)).append(",\"source_scale\":").append(format(cell.sourceScale))
                    .append(",\"output_transform\":[").append(format(cell.scale)).append(",0,0,").append(format(cell.scale)).append(',')
                    .append(format(PIVOT_X-cell.rootX*cell.scale)).append(',').append(format(PIVOT_Y-cell.rootY*cell.scale)).append(']')
                    .append(",\"output_visible_bounds\":").append(rect(bounds(pack.clips.get(state).get(col),false)))
                    .append(",\"output_body_bounds\":").append(rect(cell.outputBody))
                    .append(",\"source_override\":").append(cell.replacement).append(",\"retained_v2\":").append(cell.imported)
                    .append(",\"hash\":").append(q(Long.toUnsignedString(hash(pack.clips.get(state).get(col)),16))).append('}');
            }
            out.append("]}");
        }
        return out.append("\n  }\n}\n").toString();
    }
    static void removeStaleFrames(Path directory,Pack pack) throws Exception {
        // Only this generator's known numbered frames are managed. Unknown
        // files and source art are never touched.
        Set<String> wanted=new HashSet<>();
        for(String state:states(pack))for(int frame=1;frame<=pack.clips.get(state).size();frame++)wanted.add(state+"_"+frame+".png");
        try(var files=Files.list(directory)) {
            for(Path file:files.toList()) {
                String name=file.getFileName().toString();
                if(wanted.contains(name))continue;
                for(String state:ORDER)if(name.matches(java.util.regex.Pattern.quote(state)+"_[0-9]+\\.png")) {
                    require(file.toAbsolutePath().normalize().getParent().equals(directory.toAbsolutePath().normalize()),"Managed frame outside output directory");
                    Files.delete(file);break;
                }
            }
        }
    }
    static final class Gif implements AutoCloseable {
        final ImageWriter writer=ImageIO.getImageWritersBySuffix("gif").next();final ImageOutputStream output;boolean first=true;
        Gif(Path path)throws Exception{output=ImageIO.createImageOutputStream(path.toFile());writer.setOutput(output);writer.prepareWriteSequence(null);}
        static IIOMetadataNode node(IIOMetadataNode parent,String name) {
            for(int i=0;i<parent.getLength();i++)if(parent.item(i).getNodeName().equals(name))return(IIOMetadataNode)parent.item(i);
            IIOMetadataNode child=new IIOMetadataNode(name);parent.appendChild(child);return child;
        }
        void add(BufferedImage image,int milliseconds)throws Exception {
            ImageWriteParam params=writer.getDefaultWriteParam();IIOMetadata metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),params);
            String format=metadata.getNativeMetadataFormatName();IIOMetadataNode root=(IIOMetadataNode)metadata.getAsTree(format),control=node(root,"GraphicControlExtension");
            control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");
            control.setAttribute("transparentColorIndex","0");control.setAttribute("delayTime",Integer.toString(Math.max(1,milliseconds/10)));
            if(first){IIOMetadataNode ext=new IIOMetadataNode("ApplicationExtension");ext.setAttribute("applicationID","NETSCAPE");ext.setAttribute("authenticationCode","2.0");
                ext.setUserObject(new byte[]{1,0,0});node(root,"ApplicationExtensions").appendChild(ext);first=false;}
            metadata.setFromTree(format,root);writer.writeToSequence(new IIOImage(image,null,metadata),params);
        }
        public void close()throws Exception{writer.endWriteSequence();output.close();writer.dispose();}
    }
    static void background(Graphics2D g,int width,int height) {
        g.setColor(new Color(14,19,29));g.fillRect(0,0,width,height);
        for(int y=0;y<height;y+=16)for(int x=0;x<width;x+=16)if((x/16+y/16)%2==0){g.setColor(new Color(22,28,40));g.fillRect(x,y,16,16);}
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,16));g.setColor(new Color(227,234,248));
    }
    static BufferedImage preview(List<Pack> packs,String state,int index) {
        int width=Math.max(512,packs.size()*384),height=376;BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();background(g,width,height);g.drawString("CHRONO RUNNER / V3 / "+state.toUpperCase(Locale.ROOT)+" / FRAME "+(index+1),20,30);
        for(int actor=0;actor<packs.size();actor++) {
            Pack pack=packs.get(actor);List<BufferedImage> clip=pack.clips.get(state);int centre=actor*384+192;
            g.setColor(new Color(80,102,124));g.drawLine(actor*384+28,300,actor*384+356,300);g.drawLine(centre,284,centre,316);
            if(clip!=null)g.drawImage(clip.get(index%clip.size()),centre-PIVOT_X*2,300-PIVOT_Y*2,SIZE*2,SIZE*2,null);
            g.setColor(new Color(227,234,248));g.drawString(pack.actor,actor*384+55,344);
        }
        g.dispose();return image;
    }
    static void previews(List<Pack> packs)throws Exception {
        Path directory=ROOT.resolve("previews");Files.createDirectories(directory);
        try(Gif all=new Gif(directory.resolve("all-animations.gif"))) {
            for(String state:ORDER) {
                if(packs.stream().noneMatch(p->p.clips.containsKey(state)))continue;
                int count=state.equals("fall")||state.equals("land")?2:6;
                try(Gif gif=new Gif(directory.resolve(state+".gif"))) {
                    for(int frame=0;frame<count;frame++) {
                        BufferedImage image=preview(packs,state,frame);int delay=state.equals("run")?80:120;
                        gif.add(image,delay);all.add(image,frame==count-1&&!LOOPS.contains(state)?state.equals("death")?1000:450:delay);
                        if(frame==Math.min(3,count-1))ImageIO.write(image,"png",directory.resolve(state+".png").toFile());
                    }
                }
            }
        }
        for(Pack pack:packs) {
            contactSheet(pack,states(pack),directory.resolve(pack.actor+"-contact-sheet.png"));
        }
    }
    static void contactSheet(Pack pack,List<String> states,Path destination)throws Exception {
        int width=6*SIZE+160,height=states.size()*(SIZE+8)+40;
        BufferedImage contact=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=contact.createGraphics();background(g,width,height);
        g.drawString(pack.actor+" / SIX AUTHORED FRAMES PER STATE",12,25);
        for(int row=0;row<states.size();row++) {
            String state=states.get(row);g.setColor(new Color(227,234,248));g.drawString(state,12,52+row*(SIZE+8));
            List<BufferedImage> clip=pack.clips.get(state);
            for(int col=0;col<clip.size();col++)g.drawImage(clip.get(col),160+col*SIZE,40+row*(SIZE+8),null);
        }
        g.dispose();Files.createDirectories(destination.getParent());ImageIO.write(contact,"png",destination.toFile());
    }
    public static void main(String[] args)throws Exception {
        List<String> arguments=Arrays.asList(args);List<String> actors=new ArrayList<>(List.of("ronaldo","do_mixi","phan_si_kien"));
        if(arguments.contains("--help")){System.out.println("Usage: java -Djava.awt.headless=true tools/PrepareIdentityAnimationPack.java [--actors=ronaldo,do_mixi,phan_si_kien] [--inspect] [--inspect-source=ronaldo:locomotion] [--overrides=path.properties] [--install] [--no-preview]");return;}
        for(String argument:arguments)if(argument.startsWith("--actors="))actors=new ArrayList<>(List.of(argument.substring(9).split(",")));
        for(String actor:actors)require(ACTORS.contains(actor),"Unknown actor: "+actor);
        Path measured=ROOT.resolve("metadata/measured-overrides.properties");if(Files.isRegularFile(measured))try(Reader input=Files.newBufferedReader(measured,StandardCharsets.UTF_8)){OVERRIDES.load(input);}
        Path config=SOURCE.resolve("slicing.properties");if(Files.isRegularFile(config))try(Reader input=Files.newBufferedReader(config,StandardCharsets.UTF_8)){OVERRIDES.load(input);}
        for(String argument:arguments)if(argument.startsWith("--overrides=")) {
            Path extra=Path.of(argument.substring(12));require(Files.isRegularFile(extra),"Missing override properties: "+extra);
            try(Reader input=Files.newBufferedReader(extra,StandardCharsets.UTF_8)){OVERRIDES.load(input);}
        }
        for(String argument:arguments)if(argument.startsWith("--components=")){measureComponents(argument.substring(13).split(":"));return;}
        for(String argument:arguments)if(argument.startsWith("--inspect-source=")) {
            String[] parts=argument.substring(17).split(":");require(parts.length==2&&ACTORS.contains(parts[0]),"Expected --inspect-source=actor:sheet");
            String[] names=switch(parts[1]){case "locomotion"->MOVE;case "combo"->COMBO;case "reaction"->REACTION;case "skills"->SKILLS;default->throw new IllegalArgumentException("Unknown source sheet: "+parts[1]);};
            inspectSheet(parts[0],readSheet(parts[0],parts[1],names));return;
        }
        boolean inspect=arguments.contains("--inspect");List<Pack> packs=new ArrayList<>();
        for(String actor:actors)packs.add(prepare(actor,inspect));if(inspect)return;
        if(arguments.contains("--install"))for(Pack pack:packs) {
            Path destination=Path.of("assets/players",pack.actor,"v3");Files.createDirectories(destination);
            removeStaleFrames(destination,pack);
            try(var files=Files.list(pack.directory)){for(Path file:files.toList())Files.copy(file,destination.resolve(file.getFileName()),StandardCopyOption.REPLACE_EXISTING);}
            System.out.println("Installed "+destination);
        }
        if(!arguments.contains("--no-preview"))previews(packs);
        String summary="PASS: "+checks+" structural checks; "+packs.size()+" actors; all authored states have 6 distinct frames; binary alpha; no edge crop; death endpoint prone.\n";
        Files.createDirectories(ROOT.resolve("metadata"));Files.writeString(ROOT.resolve("metadata/validation.txt"),summary,StandardCharsets.UTF_8);System.out.print(summary);
    }
}
