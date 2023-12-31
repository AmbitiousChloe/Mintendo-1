package views;
import java.awt.*;
import java.awt.image.BufferedImage;

import instruction.InstructionController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import model.TetrisModel;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.TetrisPiece;
import org.w3c.dom.Text;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.net.URL;
import java.util.ResourceBundle;

import static model.TetrisPiece.SQUARE_STR;


/**
 * Tetris View
 *
 * Based on the Tetris assignment in the Nifty Assignments Database, authored by Nick Parlante
 */
public class TetrisView implements Initializable {
    @FXML
    Button Left_movement, Right_movement, Rotate_movement, Down_movement;
    TetrisModel model; //reference to model
    Stage stage;
    @FXML
    MenuItem startButton, stopButton, loadButton, saveButton, newButton, changeColorModebutton,
            TextInstructionButton, CustomizeColorButton; ;//menu items for some functions
    @FXML
    CheckMenuItem backgroundButton, soundeffectButton;
    @FXML
    Label scoreLabel, gameModeLabel;
    @FXML
    Slider slider;
    @FXML
    BorderPane borderPane, secondBorderPane;
    @FXML
    AnchorPane anchorPane;
    @FXML
    BufferedImage image;
    @FXML
    Canvas canvas, secondCanvas;
    @FXML
    HBox controls;
    @FXML
    VBox vBox;
    @FXML
    GraphicsContext gc;
    @FXML
    GraphicsContext bx;
    @FXML
    GraphicsContext g; //the graphics context will be linked to the canvas
    @FXML
    RadioButton pilotButtonHuman, pilotButtonComputer;

    Boolean paused, highContrast, eyeProtection, standard, customized, changeColor;
    Timeline timeline;

    ChangeColorMode changed;
    CustomizeColor customize;

    Invoker invoker;

    int pieceWidth = 25; //width of block on display

    int i = 0;
    private double width; //height and width of canvas
    private double height;
    private TetrisPiece piece;

    private state.MusicContext mc;

    //Determine whether in the highlevel state.
    public boolean state;
    //Determine whether the silent button was clicked.
    public boolean silentstate;


    public TetrisView() {
        this.model = new TetrisModel();
        this.mc = new state.MusicContext(true,0);
        this.state = true;
        this.silentstate = false;

        highContrast = false;
        eyeProtection = false;
        standard = false;
        changeColor = false;
        customized = false;
        this.invoker = new Invoker();

    }
    /*
    Set the music, val is the index indicating with music to play.
     */
    public void setMC(int val){
        this.mc = new state.MusicContext(true, val);
        this.mc.s = true;
        transState();
        model.resume();
    }

    /**
     * Initialize interface
     */
    private void initUI(){
    }

    /**
     * Get user selection of "autopilot" or human player
     *
     * @param value toggle selector on UI
     */
    private void swapPilot(Toggle value) {
        RadioButton chk = (RadioButton)value.getToggleGroup().getSelectedToggle();
        String strVal = chk.getText();
        if (strVal.equals("Computer (Default)")){
            this.model.setAutoPilotMode();
            gameModeLabel.setText("Player is: Computer (Default)");
        } else if (strVal.equals("Human")) {
            this.model.setHumanPilotMode();
            gameModeLabel.setText("Player is: Human");
        }
        borderPane.requestFocus(); //give the focus back to the pane with the blocks.
    }

    /**
     * Update board (paint pieces and score info)
     */
    private void updateBoard() {
        if (!this.paused && !changeColor) {
            paintBoard();
            clearBoard();
            printSecondBoard();
            this.model.modelTick(TetrisModel.MoveType.DOWN);
            updateScore();
        }else if(this.customized){
            this.customize.execute();
            clearBoard();
            printSecondBoard();
            this.model.modelTick(TetrisModel.MoveType.DOWN);
            updateScore();
        }else{
            this.changed.execute();
            clearBoard();
            printSecondBoard();
            this.model.modelTick(TetrisModel.MoveType.DOWN);
            updateScore();
        }
    }

    /**
     * Update score on UI
     * if the score is over 30, the variable mc will call the DetermineState,
     *      and transfer the state to highlevel mode,
     *      the state will become false,
     *      the speed will increase.
     */

    private void updateScore() {
        if (i == 0){
            i = i + 1;
        }
        if (i != model.getScore() + + model.getCount()){
        }
        if (!this.paused) {
            scoreLabel.setText("Score is: " + model.getScore() + "\nPieces placed:" + model.getCount());
        }
        if (!this.paused && model.getScore() >= 15) {
            if (state) {
                mc.DetermineState();
                state = false;
                double val = slider.getValue();
                timeline.setRate(val * 5 / 100);
            }
        }
        i = model.getScore() + model.getCount();
    }

    //call the TransitionToState and play or stop the sound.
    public void transState(){
        mc.TransitionToState(mc.current);
    }

    /**
     * Methods to calibrate sizes of pixels relative to board size
     */
    public final int yPixel(int y) {
        return (int) Math.round(this.height -1 - (y+1)*dY());
    }
    public final int xPixel(int x) {
        return Math.round((x)*dX());
    }
    public final float dX() {
        return( ((float)(this.width-2)) / this.model.getBoard().getWidth() );
    }
    public final float dY() {
        return( ((float)(this.height-2)) / this.model.getBoard().getHeight() );
    }

    /**
     * Draw the board
     */
    @FXML
    public void paintBoard() {
        // Draw a rectangle around the whole screen
        gc.setStroke(Color.rgb(242, 244, 250));
        gc.setFill(Color.rgb(242, 244, 250));
        gc.fillRect(0, 0, this.width-1, this.height-1);

        // Draw the line separating the top area on the screen
        gc.setStroke(Color.BLACK);
        int spacerY = yPixel(this.model.getBoard().getHeight() - TetrisModel.BUFFERZONE - 1);
        gc.strokeLine(0, spacerY, this.width-1, spacerY);


        // Factor a few things out to help the optimizer
        final int dx = Math.round(dX()-2);
        final int dy = Math.round(dY()-2);
        final int bWidth = this.model.getBoard().getWidth();
        int x, y;
        // Loop through and draw all the blocks; sizes of blocks are calibrated relative to screen size
        for (x=0; x<bWidth; x++) {
            int left = xPixel(x);	// the left pixel
            // draw from 0 up to the col height
            final int yHeight = this.model.getBoard().getColumnHeight(x);
            for (y=0; y<yHeight; y++) {
                if (this.model.getBoard().getGrid(x, y)) {
                    gc.setFill(Color.rgb(104, 138, 237));
                    gc.fillRect(left+1, yPixel(y)+1, dx, dy);
                    gc.setFill(Color.rgb(242, 244, 250));
                }
            }
        }
    }
    //clear the second board
    public void clearBoard() {
        bx.setFill(Color.BLACK);
        bx.fillRect(50, 0, this.width - 1, this.height - 1);
    }

    // print the second board
    public void printSecondBoard(){
        final int dx = Math.round(dX()-2);
        final int dy = Math.round(dY()-2);
        int bWidth = 30;
        int x, y;
        // Loop through and draw all the blocks; sizes of blocks are calibrated relative to screen size
        for (x=0; x<bWidth; x++) {
            int left = xPixel(x);	// the left pixel
            // draw from 0 up to the col height
            final int yHeight = 50;
            for (y=0; y<yHeight; y++) {
                if (this.model.secondGetBoard().secondGetGrid(x, y)) {
                    bx.setFill(Color.RED);
                    bx.fillRect(left+40, yPixel(y), dx, dy);;
                    bx.setFill(Color.GREEN);
                }
            }
        }
    }

    /**
     * Create the view to save a board to a file
     */
    @FXML
    private void createSaveView(){
        SaveView saveView = new SaveView(this);
    }

    /**
     * Create the view to select a board to load
     */
    @FXML
    private void createLoadView(){
        LoadView loadView = new LoadView(this);
    }
    //Create a new setting page
    @FXML
    private void openSettingsPage(){
        this.model.stopGame();
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Settings.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(scene);
            stage.show();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Controller method to let the user enter the theme setting by clicking on the button.
    At that time, the current music will be stopped.
     */
    @FXML
    private void openAudioSettingsPage(){
        this.model.stopGame();
        try{
            this.mc.s = false;
            transState();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ChangeTheme.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Theme settings");
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setOnCloseRequest(event -> {
                this.mc.s = true;
                transState();
                this.model.resume();
            });
            stage.show();
            ChangeThemeController controller = loader.getController();
            controller.view = this;
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @FXML
    private void openInstruction(){
        this.model.stopGame();
        try{
            this.mc.s = false;
            transState();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/instruction/instruction.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Instuctions");
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            stage.setOnCloseRequest(event -> {
                this.mc.s = true;
                transState();
                this.model.resume();
            });
        }catch(Exception e){
            e.printStackTrace();
        }
    }



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.paused = false;
        this.width = this.model.getWidth() * pieceWidth + 2;
        this.height = this.model.getHeight() * pieceWidth + 2;

        //add canvas
        canvas.setWidth(this.width);
        canvas.setHeight(this.height);
        gc = canvas.getGraphicsContext2D();

        //add second canvas
        secondCanvas.setHeight(100);
        secondCanvas.setWidth(200);
        bx = secondCanvas.getGraphicsContext2D();
        g = secondCanvas.getGraphicsContext2D();

        final ToggleGroup toggleGroup = new ToggleGroup();

        pilotButtonHuman.setToggleGroup(toggleGroup);
        pilotButtonHuman.setUserData(Color.SALMON);

        pilotButtonComputer.setToggleGroup(toggleGroup);
        pilotButtonComputer.setUserData(Color.SALMON);

        toggleGroup.selectedToggleProperty().addListener((observable, oldVal, newVal) -> swapPilot(newVal));

        //timeline structures the animation, and speed between application "ticks"
        timeline = new Timeline(new KeyFrame(Duration.seconds(0.25), e -> updateBoard()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        //configure this such that you start a new game when the user hits the newButton
        //Make sure to return the focus to the borderPane once you're done!

        //if silentstate is false, set the current state to normalLevelState
        //if mc.s is false, turn on the music
        //if mc.s is true, turn off the music.
        //set the speed to the normal speed.
        newButton.setOnAction(e -> {
            if(!silentstate) {
                mc.current = new state.NormalLevelState();
                if (!mc.s) {
                    mc.s = true;
                    transState();
                } else {
                    mc.sound.stop();
                    transState();
                }
                this.state = true;
            }
            double val = slider.getValue();
            timeline.setRate(val * 3 / 100);
            model.newGame();
        });

        //configure this such that you restart the game when the user hits the startButton
        //Make sure to return the focus to the borderPane once you're done!
        //if silentstate is false and the mc.s is false,
        //play the music and set mc.s is true.
        startButton.setOnAction(e -> {
            if(!silentstate){
                if(!mc.s){
                    mc.s = true;
                    transState();
                }
            }
            model.resume();
        });

        //configure this such that you pause the game when the user hits the stopButton
        //Make sure to return the focus to the borderPane once you're done!
        //if the silentstate is false, set the mc.s is false,
        //and trun off the music.
        stopButton.setOnAction(e -> {
            if(!silentstate) {
                mc.s = false;
                transState();
            }
            model.stopGame();
        });

        //if the backgroundButton is selected, the slientstate is true,
        // and the background sound will be turned off.
        //if the backgroundButton is unselected,
        // the silentstate is false,
        // the background sound will be turned on.
        backgroundButton.setOnAction(e -> {
            if(backgroundButton.isSelected()){
                silentstate = true;
                mc.s = false;
                transState();
            }else {
                silentstate = false;
                mc.s = true;
                transState();
            }
        });
        //if the soundeffectButton is selected, the sound effect will be turned off,
        //if the soundeffectButton is unselected, the sound effect sound will be turned on.
        soundeffectButton.setOnAction(e -> {
            model.det = !soundeffectButton.isSelected();
        });
        //configure this such that the save view pops up when the saveButton is pressed.
        //Make sure to return the focus to the borderPane once you're done!
        saveButton.setOnAction(e -> {
            createSaveView();
        });

        //configure this such that the load view pops up when the loadButton is pressed.
        //Make sure to return the focus to the borderPane once you're done!
        loadButton.setOnAction(e -> {
            createLoadView();
        });
        changeColorModebutton.setOnAction(e -> {
            changeColorMode();
        });


        CustomizeColorButton.setOnAction(e -> {
            customizeColor();
            customized = true;
        });
        Left_movement.setOnAction(e -> {
            model.modelTick(TetrisModel.MoveType.LEFT);
        });

        Right_movement.setOnAction(e -> {
            model.modelTick(TetrisModel.MoveType.RIGHT);
        });
        Down_movement.setOnAction(e -> {
            model.modelTick(TetrisModel.MoveType.DROP);
        });
        Rotate_movement.setOnAction(e -> {
            model.modelTick(TetrisModel.MoveType.ROTATE);
        });
        //configure this such that you adjust the speed of the timeline to a value that
        //ranges between 0 and 3 times the default rate per model tick.  Make sure to return the
        //focus to the borderPane once you're done!
        slider.setOnMouseReleased(e -> {
            double val = slider.getValue();
            timeline.setRate(val * 3 / 100);
        });
        //configure this such that you can use controls to rotate and place pieces as you like!!
        //You'll want to respond to tie key presses to these moves:
        // TetrisModel.MoveType.DROP, TetrisModel.MoveType.ROTATE, TetrisModel.MoveType.LEFT
        //and TetrisModel.MoveType.RIGHT
        //make sure that you don't let the human control the board
        //if the autopilot is on, however.
        borderPane.setOnKeyReleased(k -> {
            switch (k.getCode()) {
                case S -> model.modelTick(TetrisModel.MoveType.DROP);
                case A -> model.modelTick(TetrisModel.MoveType.LEFT);
                case D -> model.modelTick(TetrisModel.MoveType.RIGHT);
                case R -> model.modelTick(TetrisModel.MoveType.ROTATE);
            }
        });
        //play the sound
        transState();
        this.model.startGame(); //begin
    }
    private void changeColorMode(){
        this.changed = new ChangeColorMode(this);
    }
    private void customizeColor(){
        this.customize = new CustomizeColor(this);
        this.customized = true;
    }

    private void gameInstruction(){
        //
    }

    public double getWidth(){return this.width;}
    public double getHeight(){return this.height;}
}