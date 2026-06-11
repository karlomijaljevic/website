# Website

Code that is used to run my [website](https://karlo.mijaljevic.xyz/). It is
primarily written in Java on the backend and simple HTML and CSS with the Qute
templating engine for the frontend. This website is server side rendered for
simplicity due to it being just a simple blog page. Javascript is avoided at
all costs. The application server used is Quarkus. Blog and static file state is
held in a flat in-memory index that is the single source of truth; the blog and
image directories are watched by scheduled file-watching schedulers and the
created/updated timestamps are derived from the filesystem.

Technology stack:
1. Java
2. Quarkus
3. HTML
4. CSS