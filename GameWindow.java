import Characters.Character;
import java.awt.*;
import javax.swing.*;

public class GameWindow extends JFrame {

    public static final int WIDTH  = 1280;
    public static final int HEIGHT = 720;

    private CardLayout cardLayout;
    private JPanel root;

    public static final String SCREEN_LOADING   = "LOADING";
    public static final String SCREEN_MAIN      = "MAIN";
    public static final String SCREEN_CHARACTER = "CHARACTER";
    public static final String SCREEN_SETTINGS  = "SETTINGS";
    public static final String SCREEN_GAME      = "GAME";
    public static final String SCREEN_BATTLE    = "BATTLE";

    private LoadingScreen loadingScreen;
    private GameScreen gameScreen;
    private BattleScreen battleScreen;

    private String selectedCharacter;

    public GameWindow() {
        setTitle("Great Ruins of Khai");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        root = new JPanel(cardLayout);
        root.setBackground(Color.BLACK);

        loadingScreen = new LoadingScreen(this);
        gameScreen    = new GameScreen(this);
        battleScreen  = new BattleScreen(this, gameScreen);

        root.add(loadingScreen,                   SCREEN_LOADING);
        root.add(new MainMenuScreen(this),        SCREEN_MAIN);
        root.add(new CharacterSelectScreen(this), SCREEN_CHARACTER);
        root.add(new SettingsScreen(this),        SCREEN_SETTINGS);
        root.add(gameScreen,                      SCREEN_GAME);
        root.add(battleScreen,                    SCREEN_BATTLE);

        add(root);
        setVisible(true);
    }

    public void setSelectedCharacter(String name) {
        this.selectedCharacter = name;
        battleScreen.setSelectedCharacter(name);
        gameScreen.setSelectedCharacter(name);
    }

    public String getSelectedCharacter() {
        return selectedCharacter;
    }

    public void showMainMenu()        { cardLayout.show(root, SCREEN_MAIN); }
    public void showCharacterSelect() { cardLayout.show(root, SCREEN_CHARACTER); }
    public void showSettings()        { cardLayout.show(root, SCREEN_SETTINGS); }
    public void showGameScreen()      { cardLayout.show(root, SCREEN_GAME); }

    public void showGame() {
        cardLayout.show(root, SCREEN_LOADING);
        loadingScreen.startLoading(() -> {
            cardLayout.show(root, SCREEN_GAME);
            gameScreen.startGame();
        });
    }

    public void showBattle(Enemy mapEnemy, Character battleEnemy, GameScreen gs) {
        cardLayout.show(root, SCREEN_BATTLE);
        battleScreen.requestFocusInWindow();
        battleScreen.startBattle(
            gs.getPlayerCharacter(),
            battleEnemy,
            () -> {
                boolean won = battleScreen.playerWon();
                gs.onBattleEnd(mapEnemy, won);
            }
        );
    }
}