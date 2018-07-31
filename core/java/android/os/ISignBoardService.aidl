package android.os;

/**{@hide}*/
interface ISignBoardService {
	void removeAllViews();
	void initViews();
	void refreshViews();

	void setQuickToolsEnabled(boolean enabled);
	void sendQuickToolsAction(String action);
	void setFlashlightEnabled(boolean enabled);

	boolean isFlashlightEnabled();

}
