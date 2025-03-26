package ca.corbett.musicplayer.ui;

/**
 * UI classes can implement this interface if they are capable
 * of reloading themselves when user preferences change. For example,
 * if your UI component is affected by a change in the current theme,
 * or any other user-configurable property, then you can register
 * with the global ReloadUIAction to receive notification when
 * these properties have changed. You can respond to that message
 * by redrawing whatever UI elements may have been changed.
 * <p>
 * This interface and a global UIReloadAction is a much cleaner
 * approach to this than what I was doing before, which was having
 * the AppConfig class manually reach out one by one to each
 * affected class and telling them to reload. This way, I don't
 * have to remember to update the AppConfig's save() method to
 * include a new UI component. I just need to remember to register
 * the new UI component with ReloadUIAction and then everything
 * after that happens automatically.
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-25
 */
public interface UIReloadable {

    /**
     * Invoked by ReloadUIAction when it's time to reload the UI.
     * AppConfig should be queried for the latest state of all
     * user-configurable application settings.
     */
    void reloadUI();
}
