# helma.Group

A distributed group API for replication across multiple Helma instances. Built on JGroups.

```javascript
app.addRepository("modules/helma/Group.js");
```

## Constructor

```javascript
var group = new helma.Group(javaGroup);
```

`javaGroup` is a `JChannel` instance from JGroups.

## API

### Get / Set Properties

```javascript
group.set(key, value, sendMode);
group.get(key);
group.remove(key, sendMode);

group.listChildren();
group.listProperties();
group.countChildren();
group.countProperties();
```

### Call Remote Function

```javascript
group.callFunction(method, argArray, sendMode);
```

### Get Underlying Java Object

```javascript
group.getJavaObject();
```

## Send Modes

| Constant | Behavior |
|---|---|
| `helma.Group.GroupObject.GET_FIRST` | Return first response |
| `helma.Group.GroupObject.GET_ALL` | Wait for all members |
| `helma.Group.GroupObject.GET_MAJORITY` | Wait for majority |
| `helma.Group.GroupObject.GET_ABS_MAJORITY` | Absolute majority |
| `helma.Group.GroupObject.GET_NONE` | Fire-and-forget |
| `helma.Group.GroupObject.DEFAULT_GET` | Default mode |

## GroupObject Sub-class

`new helma.Group.GroupObject(jGroupObject)` wraps a JGroups channel for property-style access:

```javascript
go.set(key, val);
go.get(key);
go.remove(key);
go.getProperty(key);
go.listChildren();
go.isLocal();
go.toJSObject();
```

## Manager

`new helma.Group.Manager()` for group lifecycle management:

```javascript
var mgr = new helma.Group.Manager();
mgr.connect("groupName");
mgr.disconnect("groupName");
mgr.listMembers("groupName");
mgr.isConnected("groupName");
mgr.size("groupName");
```

## Use Case

Replicated cache invalidation across a Helma cluster:

```javascript
var group = new helma.Group(channel);
group.callFunction("Root.clearCache", [], helma.Group.GroupObject.GET_ALL);
```

## See Also

- [JGroups documentation](https://www.jgroups.org/manual/index.html)
- [`modules/helma/Group.js`](https://github.com/DionixGmbH/helma/src/branch/main/modules/helma/Group.js)
