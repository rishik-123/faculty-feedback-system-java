Faculty Feedback System — Java Project

A console-based Java application that collects anonymous student feedback for faculty members and generates visual analysis using JFreeChart.
This project demonstrates Java OOP, exception handling, MySQL integration, and data visualization.

 Features
 Student Module

Students enter their basic details.

Select faculty from their department.

Give ratings across multiple criteria.

Feedback is stored anonymously in MySQL.

 HOD Module

Secure login for HOD.

View department-wise feedback.

Auto-generated bar charts using JFreeChart.

Helps evaluate faculty performance.

 Tech Stack
Backend

Java (Core Java + OOP + Collections + Exception Handling)

Database

MySQL

JDBC (Java Database Connectivity)

Libraries Used

JFreeChart (for graphs)

JCommon

MySQL Connector/J

How to Run the Project
1. Clone the repository
git clone https://github.com/yourusername/FacultyFeedbackSystem.git
cd FacultyFeedbackSystem

2. Add Libraries

Ensure the jars in the lib/ folder are added to your classpath:

mysql-connector-java

jfreechart

jcommon

3. Create MySQL Database
CREATE DATABASE faculty_feedback;
USE faculty_feedback;

-- Run the tables from Database.java OR manually create tables in MySQL

4. Compile the project
javac -cp "lib/*" src/*.java

5. Run the project
java -cp "lib/*:src/" FacultyFeedbackSystemManager

 Output Example

Student gives multiple ratings

HOD views feedback summary

Bar chart image generated using JFreeChart

 Learning Outcomes

By building this project, I learned:

Java OOP and multi-class architecture

Custom exceptions

Connecting Java with MySQL

Reading secure password input

Generating charts using external libraries

Handling real-world project structure

 Contributions

Pull requests and issues are welcome!

 License

This project is open-source under the MIT License.
check the documentation for the output image and the souce code
