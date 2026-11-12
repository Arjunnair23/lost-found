# Campus Lost and Found

A web application that helps a campus community report lost or found belongings, browse reports, and contact the people who posted them.

## Languages and technologies

- Java 21 with Jakarta Servlet 6.1
- HTML, CSS, and JavaScript for the web interface
- JSP for the dashboard
- XML for Maven and web application configuration
- Maven for building and testing; Apache Tomcat 11 for deployment
- JSON files for local application data

## What it does

- Lets students register, sign in, and sign out.
- Supports submitting lost and found reports and browsing/filtering reports.
- Provides a personal reports page for editing, deleting, and updating report status.
- Restricts report changes to signed-in users and their own reports.
- Stores user and report data outside the web application in JSON files.

## Install and run

See [INSTALL.md](INSTALL.md) for prerequisites, build/test commands, Tomcat deployment, and data-storage configuration.

## Notes

Public registration creates student accounts; there is no default administrator account. Data is stored locally as JSON and is intended for a single application instance, not multi-instance production use.
