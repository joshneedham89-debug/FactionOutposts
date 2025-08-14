
package com.yourname.factionoutposts;

public class CaptureState {
    public int progress = 0;
    public String lastContestingFaction = null;

    public void reset(){ progress = 0; lastContestingFaction = null; }
    public void decay(int amount){
        progress -= amount;
        if (progress < 0) progress = 0;
    }
}
