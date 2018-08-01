package android.os;

/**{@hide}*/
interface ISignBoardService {
	void removeAllViews();
	void initViews();
	void refreshViews();

	void setQuickToolsEnabled(boolean enabled);
	void setFlashlightEnabled(boolean enabled);
	void setMusicControllerEnabled(boolean enabled);

	void sendQuickToolsAction(String key);
	void sendMusicControllerAction(String key);

	boolean isFlashlightEnabled();

}
