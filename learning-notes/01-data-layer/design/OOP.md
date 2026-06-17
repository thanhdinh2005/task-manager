# Self-Review: The 4 Pillars of OOP

### 1. Purpose
Reviewing the core of Object-Oriented Programming (OOP). Before diving into the pillars, I need to remember the basics:
* A **Class** is a blueprint (the idea of a "Student").
* An **Object** is the real thing created from that blueprint (a real student named "A").

### 2. Encapsulation (Data Hiding)
* **The Concept:** Wrapping data and behaviors together and restricting direct access from the outside.
* **The Analogy:** Think of personal information. You have public info (like your name or age) that anyone can see, but private info (like your bank account balance or web history) that is locked away.
* **How it works in code:** This is the heart of a **Rich Domain Model**. We make fields `private` and remove public setters so outside classes can't mess up the entity's internal state.

### 3. Abstraction (Hiding Complexity)
* **The Concept:** Showing only the essential features of an object and hiding the messy background details.
* **The Analogy:** A TV remote. When I press the "ON" button, the remote doesn't know the complex electronics inside the TV. It just sends a signal, and the TV knows how to handle the rest.
* **How it works in code:** We use **Interfaces**. A Controller calls `paymentProcessor.process()`. The Controller (the remote) doesn't care *how* the payment is processed; it just knows the Interface provides that "button."

### 4. Inheritance (Reusability)
* **The Concept:** A class acquiring properties from another class to avoid redundant code.
* **The Analogy:** Animals. Birds and humans share common traits (we are all animals), but birds have specific traits like flying (without the help of tools!).
* **How it works in code:** Using `extends`. Just like how we made our entities extend an abstract `BaseEntity` to automatically inherit the `id` and `createdAt` fields.

### 5. Polymorphism (Many Forms)
* **The Concept:** The ability of a single method or object to take on different forms. It comes in two flavors:
    * **Overloading:** Methods with the same name but different inputs (e.g., different quantities or types of parameters) in the same class.
    * **Overriding:** One method implemented in different ways across child classes.
* **The Analogy (Overriding):** A payment system. We have a general "pay" action, but it takes different forms depending on the gateway (MB Bank, VNPAY, etc.). Each gateway *overrides* the standard "pay" method with its own specific logic.

### 6. Lesson Learned
OOP isn't just theory; it's a survival toolkit for writing maintainable code. By combining these 4 pillars, I can write code that is secure (Encapsulation), easy to use (Abstraction), avoids repetition (Inheritance), and is highly flexible (Polymorphism).