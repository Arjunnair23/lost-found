<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    if (session.getAttribute("username") == null) {
        response.sendRedirect("login.html");
        return;
    }

    String username = (String) session.getAttribute("username");
    String displayName = username.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Manage your campus lost and found reports.">
    <title>Dashboard | Campus Lost &amp; Found</title>
    <link rel="stylesheet" href="style.css?v=20261112-2">
</head>
<body class="dashboard-page">
    <nav class="navbar dashboard-navbar" aria-label="Main navigation">
        <a class="logo dashboard-brand" href="index.html" aria-label="Campus Lost and Found home">
            Campus <span>Lost &amp; Found</span>
        </a>
        <div class="nav-links dashboard-nav-actions">
            <a href="view-items.html">Browse lost items</a>
            <a href="found-items.html">Browse found items</a>
            <a href="my-items.html">My reports</a>
            <form action="logout" method="post">
                <button type="submit" class="nav-logout">Log out</button>
            </form>
        </div>
    </nav>

    <main class="dashboard-main">
        <div class="dashboard-notice" id="dashboardNotice" role="status" aria-live="polite" hidden>
            <span class="notice-check" aria-hidden="true">✓</span>
            <span id="dashboardNoticeMessage">Your report has been submitted.</span>
            <button class="notice-dismiss" type="button" aria-label="Dismiss message">×</button>
        </div>

        <section class="dashboard-hero" aria-labelledby="dashboardGreeting">
            <div class="dashboard-welcome">
                <p class="dashboard-eyebrow"><span class="online-dot" aria-hidden="true"></span> YOUR CAMPUS COMMUNITY</p>
                <h1 id="dashboardGreeting">Welcome, <span><%= displayName %></span></h1>
                <p class="dashboard-intro">A missing item can interrupt your day. Start a report or browse what the campus community has found.</p>
                <p class="dashboard-date" id="dashboardDate"></p>
                <div class="dashboard-hero-actions">
                    <a class="btn primary" href="add-item.html">Report a lost item <span aria-hidden="true">→</span></a>
                    <a class="btn secondary" href="view-items.html">Browse reports</a>
                </div>
            </div>
            <div class="dashboard-illustration" aria-hidden="true">
                <span class="illustration-orbit orbit-one"></span>
                <span class="illustration-orbit orbit-two"></span>
                <div class="illustration-card">
                    <span class="illustration-icon">🔎</span>
                    <span class="illustration-caption">Together, things get found</span>
                    <span class="illustration-check">✓</span>
                </div>
                <span class="illustration-spark spark-one">✦</span>
                <span class="illustration-spark spark-two">✧</span>
            </div>
        </section>

        <section class="dashboard-section" aria-labelledby="dashboardActionsHeading">
            <div class="dashboard-section-heading">
                <div>
                    <p class="dashboard-section-label">QUICK ACTIONS</p>
                    <h2 id="dashboardActionsHeading">What would you like to do?</h2>
                </div>
                <p>Choose a service to get started.</p>
            </div>

            <div class="dashboard-actions">
                <a class="dashboard-action-card action-blue" href="add-item.html">
                    <span class="action-icon" aria-hidden="true">⌕</span>
                    <span class="action-card-copy">
                        <strong>Report a lost item</strong>
                        <span>Tell the community what you are looking for.</span>
                    </span>
                    <span class="action-arrow" aria-hidden="true">↗</span>
                </a>
                <a class="dashboard-action-card action-violet" href="report-found.html">
                    <span class="action-icon" aria-hidden="true">✦</span>
                    <span class="action-card-copy">
                        <strong>Report a found item</strong>
                        <span>Help return a campus item to its owner.</span>
                    </span>
                    <span class="action-arrow" aria-hidden="true">↗</span>
                </a>
                <a class="dashboard-action-card action-mint" href="view-items.html">
                    <span class="action-icon" aria-hidden="true">☷</span>
                    <span class="action-card-copy">
                        <strong>Browse lost items</strong>
                        <span>Search the latest lost-item reports.</span>
                    </span>
                    <span class="action-arrow" aria-hidden="true">↗</span>
                </a>
                <a class="dashboard-action-card action-amber" href="my-items.html">
                    <span class="action-icon" aria-hidden="true">▤</span>
                    <span class="action-card-copy">
                        <strong>View my reports</strong>
                        <span>Review the lost items you have reported.</span>
                    </span>
                    <span class="action-arrow" aria-hidden="true">↗</span>
                </a>
            </div>
        </section>

        <aside class="dashboard-tip">
            <span class="tip-icon" aria-hidden="true">💡</span>
            <div>
                <p class="tip-label">A HELPFUL TIP</p>
                <h2>Include a few identifying details.</h2>
                <p>Colour, location and date can help someone recognize an item quickly.</p>
            </div>
            <a class="tip-link" href="add-item.html">Create a report <span aria-hidden="true">→</span></a>
        </aside>

        <footer class="dashboard-footer">
            <span>© 2026 Campus Lost &amp; Found</span>
            <a href="index.html">Campus community support</a>
        </footer>
    </main>

    <script src="script.js?v=20261112-2"></script>
    <script src="csrf.js?v=20261112-2"></script>
</body>
</html>
