# class.properties

`class.properties` maps Java class names to Helma prototype names. Lives at `apps/<app>/class.properties`. Used to let Helma know which Java class should be wrapped with which HopObject prototype.

## What It's For

When you have a Java extension that returns a custom Java class, Helma normally wraps it as a `NativeJavaObject`. To make it appear as a HopObject with a JavaScript prototype attached, register the class-to-prototype mapping:

```properties
com.example.MyJavaClass = MyPrototype
```

Now when a method returns `MyJavaClass`, Helma wraps it as a `HopObject` of prototype `MyPrototype`, so all the JS functions and skin templates of that prototype apply.

## Use Cases

### Wrap a Java POJO as a HopObject

Java side:

```java
package com.example;

public class Address {
    public String street;
    public String city;
    public String country;

    public String getFullAddress() {
        return street + ", " + city + ", " + country;
    }
}
```

Register:

```properties
# class.properties
com.example.Address = Address
```

Now in JavaScript:

```javascript
function showAddress() {
    var addr = new Packages.com.example.Address();
    addr.street = "Main St";
    addr.city = "Vienna";

    // addr is now a HopObject of prototype "Address"
    // You can render its skin:
    addr.renderSkin("main");
}
```

`Address/main.skin`:

```html
<address>
    <%= this.street %><br>
    <%= this.city %>, <%= this.country %>
</address>
```

The Java property accessors (`addr.street` → `addr.getStreet()`) work alongside any JS methods you add via `Address/*.js`.

### Map an existing extension class

Helma's bundled image, mail, file extensions are pre-registered. But if you have your own Java library:

```properties
com.example.MyService = MyService
```

Then in `apps/<app>/MyService/main.js` you can add JS methods to the prototype, and they're available on any `new MyService()` instance.

## Lookup

When a Java object enters the JS scope (return value from a method, parameter wrapping, etc.), Helma asks `Application.getPrototypeNameForClass(className)`:

1. Checks `class.properties` for the exact class name
2. Walks up the Java class hierarchy if not found
3. Falls back to `NativeJavaObject` wrapping if no mapping found

## Limitations

- One Java class can map to only one prototype.
- The mapping is read once at app startup. Changes require an app restart.
- The HopObject API (`size()`, `get(i)`, `add()`, `remove()`) doesn't apply to non-Node Java classes — only `_prototype`, JS methods, skin rendering work.

## Example

```properties
# class.properties for an app that wraps custom services

com.example.SearchResult = SearchResult
com.example.RecommendationResult = Recommendation
com.example.ApiResponse = ApiResponse
```

Define corresponding prototype directories:

```
apps/myapp/
├── SearchResult/
│   ├── main.skin
│   └── main.js
├── Recommendation/
│   └── ...
└── ApiResponse/
    └── ...
```

## See Also

- [Java Interoperability](../scripting/java-interop.md)
- [Concepts: Prototypes & Inheritance](../concepts/prototypes.md)
- [Extensions: Writing Java Extensions](../extensions/writing-extensions.md)
