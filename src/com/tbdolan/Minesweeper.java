package com.tbdolan;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.event.ActionEvent;

import java.util.ArrayList;
import java.util.List;

public class Minesweeper extends Application {

    private final int WIDTH = 800;
    private final int HEIGHT = 700;
    private final int Y_OFFSET = 100;
    private final int TILE_SIZE = 40;
    private final int X = WIDTH/TILE_SIZE;
    private final int Y = (HEIGHT-Y_OFFSET)/TILE_SIZE;
    private final int NUM_TILES = X*Y;

    private Tile[][] board = new Tile[X][Y];
    private boolean playable;
    private int numOpened;
    private int numMines;
    private int minesLeft = 0;
    Pane root;
    Timeline fiveSecondsWonder;

    Scene scene;
    private Label mineCount;
    private short time;

    private Parent createContent() {
        time = 0;
        root = new Pane();

        Label timer = new Label();
        timer.setText("Time: " + time);
        timer.setFont(Font.font(25));
        timer.setTranslateX(250);

        fiveSecondsWonder = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                time++;
                timer.setText("Time: " + time);
            }
        }));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();

        numMines = 0;
        playable = true;
        numOpened = 0;
        root.setPrefSize(WIDTH, HEIGHT);
        for(int x = 0; x < X; x++) {
            for(int y = 0; y < Y; y++) {
                Tile tile = new Tile(x, y, Math.random() < 0.1);
                board[x][y] = tile;
                if(tile.hasMine) {
                    numMines++;
                }
                root.getChildren().add(tile);
            }
        }

        minesLeft = numMines;

        mineCount = new Label();
        mineCount.setText("Mines: " + minesLeft);
        mineCount.setFont(Font.font(25));
        mineCount.setTranslateX(450);
        root.getChildren().addAll(timer, mineCount);

        int numBomb = 0;
        for(int x = 0; x < X; x++) {
            for (int y = 0; y < Y; y++) {
                if(!board[x][y].hasMine) {
                   numBomb = 0;
                   List<Tile> neighbors = getNeighbors(board[x][y]);
                   for(Tile t : neighbors) {
                       if (t.hasMine) {
                           numBomb++;
                       }
                   }
                   if(numBomb != 0) {
                       board[x][y].changeText(Integer.toString(numBomb));
                   }
                }
            }
        }
        return root;
    }

    private List<Tile> getNeighbors(Tile tile) {
        int[] xDir = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] yDir = {-1, -1, -1, 0, 0, 1, 1, 1};
        int newX = 0;
        int newY = 0;

        ArrayList<Tile> neighbors = new ArrayList<>();

        for(int i = 0; i < 8; i++) {
            newX = tile.getX() + xDir[i];
            newY = tile.getY() + yDir[i];
            if(newX >= 0 && newX < X && newY >= 0 && newY < Y) {
                neighbors.add(board[newX][newY]);
            }
        }
        return neighbors;
    }

    private void checkPlayability() {
        if(NUM_TILES - numOpened <= numMines) {
            playable = false;
        }
    }

    private class Tile extends StackPane {
        private int x, y;
        private boolean hasMine;
        private boolean isOpened;
        private Rectangle border;
        private Text text;
        private boolean isFlagged;
        ImagePattern flag;
        ImagePattern mine;

        public Tile(int x, int y, boolean hasMine) {
            this.x = x;
            this.y = y;
            this.hasMine = hasMine;

            flag = new ImagePattern(new Image("file:flag.png"));
            mine = new ImagePattern(new Image("file:mine.png"));
            isOpened = false;
            isFlagged = false;

            text = new Text();
            text.setFont(Font.font(32));
            text.setText(hasMine ? "X" : "");
            text.setTextAlignment(TextAlignment.CENTER);
            text.setVisible(false);

            border = new Rectangle(TILE_SIZE-2, TILE_SIZE-2);
            border.setFill(Color.LIGHTGRAY);
            setTranslateX(x*TILE_SIZE);
            setTranslateY(y*TILE_SIZE+Y_OFFSET);
            getChildren().addAll(border, text);

            setOnMouseClicked(e -> {
                if(e.getButton() == MouseButton.PRIMARY) {
                    open();
                    checkPlayability();
                    if(!playable) {
                        AlertBox.display("Minesweeper", "You Win!");
                        fiveSecondsWonder.pause();
                        scene.setRoot(createContent());
                        return;
                    }
                }
                else if(e.getButton() == MouseButton.SECONDARY) {
                    if(isOpened) {
                        return;
                    }
                    if(!isFlagged) {
                        border.setFill(flag);
                    }
                    else {
                        border.setFill(Color.LIGHTGRAY);
                    }
                    isFlagged = !isFlagged;
                    minesLeft =  isFlagged ? --minesLeft : ++minesLeft;
                    mineCount.setText("Mines: " + minesLeft);
                }
            });
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void changeText(String t) {
            text.setText(t);
        }

        //DFS-ish approach to open all neighboring blank tiles
        private void open() {
            if(isOpened) {
                return;
            }

            isOpened = true;
            text.setVisible(true);
            border.setFill(null);
            System.out.println(numOpened);

            if(hasMine) {
                for(int x = 0; x < X; x++) {
                    for(int y = 0; y < Y; y++) {
                        if(board[x][y].hasMine) {
                            board[x][y].border.setFill(mine);
                            board[x][y].text.setVisible(false);
                        }
                    }
                }
                AlertBox.display("Minesweeper", "Game Over");
                fiveSecondsWonder.pause();
                scene.setRoot(createContent());
                //restart();
                return;
            }
            numOpened++;
            if(text.getText() != "") {
                return;
            }
            List<Tile> neighbors = getNeighbors(this);
            for(Tile t : neighbors) {
                if(!t.hasMine) {
                    t.open();
                }
            }
        }
    }



    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("Minesweeper");
        scene = new Scene(createContent());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
