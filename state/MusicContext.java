package state;

import model.Sound2;
import model.TetrisModel;

public class MusicContext {
    public Sound2 sound;
    //public  int score;
    public MusicState current;
    //determine whether the music play or not.
    public boolean s;
    public int theme_idx;
    public MusicContext(boolean s, int theme_idx) {
        this.sound = new Sound2();
        this.s = s;
        this.theme_idx = theme_idx;
        this.current = new NormalLevelState();
    }
    //Given a MusicState object variable,
    //Assign the variable to the current
    //The variable current will call the EnterState method,
    //Then the certain MusicState object will run the EnterState method.
    public void TransitionToState(MusicState state){
        current = state;
        current.EnterState(this, theme_idx);
    }
    //This method can call the Determine method in the MusicState object,
    //Therefore, the current state can transfer to the other state.
    public void DetermineState(){
        current.Determine(this);
    }



}
