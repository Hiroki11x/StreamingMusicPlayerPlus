package bmw.awa.awabmw;

interface MusicService {
    public void mediaplayerPrepare(String args);
    public String calcDuration(boolean isFirstCall);
    public void startOrStop();
    public void setLoopStateService();
    public void getIntentContents(Intent intent);
    public MediaPlayer getMediaPlayer();
}
