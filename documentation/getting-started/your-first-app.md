# Your First Application

This page builds two applications: a "Hello, World" and a minimal blog. Both are 100% self-contained — you create files, save them, and reload the page.

## Hello, World

### 1. Register the application

Edit `apps.properties` and add a single line:

```properties
hello
```

That declares an application named `hello` and tells the [ApplicationManager](../concepts/architecture.md) to scan `apps/hello/` for code.

### 2. Create the prototype directory

```bash
mkdir -p apps/hello/Root
```

The `Root` directory holds the root [prototype](../concepts/prototypes.md). Every Helma application has a Root prototype — it's the object whose properties become the URL space of the app.

### 3. Add an action

Create `apps/hello/Root/main.js`:

```javascript
function main_action() {
    res.write("Hello, World!");
}
```

Functions ending in `_action` are mapped to URL segments. `main_action` is the default action invoked when no explicit action is in the URL. See [Actions](../framework/actions.md) for the full resolution algorithm.

### 4. Start Helma

```bash
./gradlew run
```

### 5. Visit the app

[http://localhost:8080/hello/](http://localhost:8080/hello/)

You should see `Hello, World!`. Edit `main.js`, save, reload — the change is live with no restart.

## A Tiny Blog

We'll build a blog with two prototypes (`Root` and `Post`), an in-memory list (no DB yet), and a skin template.

### 1. Application skeleton

```bash
mkdir -p apps/blog/Root apps/blog/Post
echo "blog" >> apps.properties
```

### 2. The Post prototype

`apps/blog/Post/type.properties` — empty for now, just marks the directory as a prototype:

```properties
# Post prototype - in-memory only for now
```

`apps/blog/Post/main.js`:

```javascript
function main_action() {
    renderSkin("Post");
}
```

`apps/blog/Post/main.skin`:

```html
<article>
  <h2><% this.title %></h2>
  <p><% this.body %></p>
  <small>Posted <% this.created format="yyyy-MM-dd" %></small>
</article>
```

Inside a skin, `<% this.foo %>` is a [macro](../framework/macro-syntax.md) that renders the `foo` property of the current object. The format parameter is a Java `SimpleDateFormat` pattern.

### 3. The Root prototype

`apps/blog/Root/main.js`:

```javascript
function main_action() {
    renderSkin("Root");
}

function add_action() {
    if (req.isPost()) {
        var post = new Post();
        post.title = req.postParams.title;
        post.body  = req.postParams.body;
        post.created = new Date();
        this.add(post);                 // persist as child of Root
        res.redirect(this.href());
    }
    renderSkin("form");
}
```

`apps/blog/Root/main.skin`:

```html
<!DOCTYPE html>
<title>My Blog</title>
<h1>My Blog</h1>
<p><a href="<% this.href action="add" %>">Write a new post</a></p>
<% this.posts %>
```

`apps/blog/Root/posts.js`:

```javascript
function posts_macro() {
    for each (var post in this.list()) {     // this.list() returns children
        post.renderSkin("Post");
    }
}
```

`apps/blog/Root/form.skin`:

```html
<form method="POST">
  <p><input name="title" placeholder="title"></p>
  <p><textarea name="body" placeholder="body"></textarea></p>
  <p><button type="submit">Post</button></p>
</form>
```

### 4. Try it

Reload the blog at [http://localhost:8080/blog/](http://localhost:8080/blog/). Click "Write a new post", submit the form. Each submission becomes a `Post` HopObject and is auto-persisted to the embedded XML database in `db/blog/`. The list of posts grows.

### What just happened?

- `new Post()` creates a transient HopObject of prototype `Post`.
- `this.add(post)` makes it a child of the current Root and triggers automatic persistence.
- `this.list()` returns all children of `this` — the embedded DB stores parent/child relations automatically when no `type.properties` mapping exists.
- `<% this.posts %>` inside the Root skin invokes `Root.posts_macro` because of the `_macro` suffix convention.
- `this.href()` and `this.href("add")` generate URLs by walking the request path back to the application root.

## Next Steps

- Map `Post` to a real DB table → [Type Properties Reference](../database/type-properties.md).
- Add login/logout → [Authentication](../framework/authentication.md).
- Send the new post via email → [`helma.Mail`](../modules/helma/mail.md).
- Schedule a cleanup job nightly → [Cron Jobs](../framework/cron-jobs.md).
