package mirage.heb.events;

public enum HebEvent {
	AW_INSTANTIATION("create-augmented-world", false),
	HOLOGRAM_CREATION("new-hologram", false),
	HOLOGRAM_DISPOSING("dispose-hologram", false),
	UPDATE_HOLOGRAM_PROPERTY("update-hologram-property", true),
	EXECUTE_ACTION_ON_HOLOGRAM("execute-action-on-hologram", false);
	
	private String description;
	private boolean replaceable;
	
	private HebEvent(final String description, final boolean replaceable) {
		this.description = description;
		this.replaceable = replaceable;
	}
	
	public String description() {
		return description;
	}
	
	public boolean isReplaceable() {
		return replaceable;	
	}
}
