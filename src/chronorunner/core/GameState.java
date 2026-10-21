package chronorunner.core;

/** All top level screens the game can be in. Used as the State Pattern key. */
public enum GameState {
    MAIN_MENU,
    CREDITS,
    LEVEL_SELECT,
    SETTINGS,
    CHARACTER_SELECT,
    PLAYING,
    PAUSED,
    VICTORY,
    GAME_OVER
}
