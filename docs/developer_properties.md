<-- [Back to musicplayer documentation](../README.md)

# Developer overview: Using `swing-extras` for application configuration

We start by extending the abstract `AppProperties` class from the `swing-extras` library.
There is only one abstract method that we need to implement:

```java
protected abstract List<AbstractProperty> createInternalProperties();
```

This method will be invoked automatically as needed, and we should use it to return a
list of our application's configuration properties. The `AppProperties` class, together with
the `swing-extras` classes `PropertiesManager` and `PropertiesDialog`, will handle all 
of the UI code for us, which is very nice and makes our lives here at the application
level much easier.

You'll notice that we need to supply a custom `ExtensionManager` to the constructor of
our `AppProperties` implementation... let's not worry about that just yet. Instead,
let's focus on setting up our application properties.

## Defining properties

The `swing-extras` library contains a `ca.corbett.extras.properties` package that has many
useful classes already defined for us. Specifically, we can look at `AbstractProperty` and
the many implementations of it provided by `swing-extras`. The `AbstractProperty` class
insists that every property be supplied with a fully qualified name, in dot-separated
format. The name not only uniquely identifies the property, but helps to group it
into an organizational structure which will be highly useful to us when it comes time
to render our `PropertiesDialog`.

The general format is `[category.[subcategory.]]propertyName`

Both the top-level category name and the sub-category name are optional. If not specified,
they will default to `General` and `General`. Here are some example property names:

- `UI.windowState` - creates a property called `windowState` belonging to an implied subcategory of `General` within the `UI` top-level category.
- `UI.window.state` - creates a property called `state` in the subcategory of `window` within the top-level category of `UI`.
- `windowState` - creates a property called `windowState` in an implied top-level category of `General` with an implied subcategory of `General`
- `UI.window.state.isMaximized` - creates a property called `state.isMaximized` within the `window` subcategory in the `UI` top-level category. Note that further dots after the second one are basically ignored and are considered part of the property name. So, you can't have sub-sub-categories.

So, as an example, let's look at some properties that the musicplayer application defines:

```java
buttonSize = new EnumProperty<ButtonSize>("UI.General.buttonSize", "Control size:", ButtonSize.LARGE);
controlAlignment = new EnumProperty<ControlAlignment>("UI.General.controlAlignment", "Control alignment:", ControlAlignment.CENTER);
```

We use the `EnumProperty` to automatically create and wrap a combo box using the values of our custom enums `ButtonSize`
and `ControlAlignment`. These enums look like this:

```java
public enum ButtonSize {
    XSMALL(16, "Extra small"),
    SMALL(20, "Small"),
    NORMAL(24, "Normal"),
    LARGE(30, "Large"),
    XLARGE(36, "Huge");

    final private int buttonSize;
    final private String label;

    ButtonSize(int btnSize, String label) {
        buttonSize = btnSize;
        this.label = label;
    }

    public int getButtonSize() {
        return buttonSize;
    }

    @Override
    public String toString() {
        return label;
    }
}

public enum ControlAlignment {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right");

    private final String label;

    ControlAlignment(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
```

We can see that each enum defines both internal names in all uppercase (for example, `CENTER`), but also defines
a `toString()` to return a more human-readable label for each value. When we generate the `PropertiesDialog` later,
we can see what this looks like in action:

![PropertiesDialog in action](screenshots/config_ui.jpg)

We see that our top-level category of `UI` was used to create a tab called `UI` in the dialog. We further see
that the sub-category name we supplied, `General`, resulted in a bold-text header label being generated above
our controls. We also notice that the dropdown combo boxes generated for us automatically make use of the
`toString()` implementations in our enum classes. We didn't have to write code to generate and display the
right options... the `PropertiesManager` class handled that all for us. Nice!

With this basic approach, we can continue creating all of our application configuration properties in
the `createInternalProperties` method. The `PropertiesManager` class will do the rest!

## Saving and loading

Of course, defining properties and showing them to the user is great, but how do we handle saving and
loading of the properties? Easy! The `AppProperties` class gives us a `save()` and `load()` method,
which handles it all for us, using whatever `File` we gave it in the constructor. 

