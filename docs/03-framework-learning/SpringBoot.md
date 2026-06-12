# Self-Review: Spring Boot Auto-Configuration vs. Manual Servlets

### 1. Purpose
Reflecting on the evolution from manual server setup to modern framework magic. The goal is to understand how Spring Boot makes development easier, but also to recognize the hidden costs behind that convenience.

### 2. The Old Way
In older projects, building a web app meant working with raw Java Servlets. I had to manually install and configure a Tomcat server, map every single API route in a massive `web.xml` file, and download `.jar` files by hand. It was such a hassle and honestly a terrible developer experience.

### 3. The Current Approach
Now, Spring Boot changes everything. With just a single `@SpringBootApplication` annotation, the framework automatically sets up everything we need. We don't even install Tomcat anymore; Spring embeds Tomcat directly inside our application.

### 4. How it Works & The Trade-off
* **Classpath Scanning & Auto-Configuration:** Behind the scenes, Spring Boot scans my project's dependencies. If it sees a web library, it automatically configures a web server. If it sees MySQL, it automatically prepares a database connection.
* **The Trade-off (The Slow Startup):** As I noticed, the major downside is that the server takes too much time to start. Because Spring has to use Java Reflection to scan thousands of classes, wire dependencies together, and initialize all the Beans during boot time, it becomes heavy and slow to launch compared to a raw Java app.

### 5. Lesson Learned
There is no perfect technology. Spring Boot saves developers hundreds of hours of manual configuration, but it trades that convenience for a heavier startup footprint. Recognizing these trade-offs is crucial when choosing the right tech stack for a project.