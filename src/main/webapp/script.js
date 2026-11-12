// ==============================
// REGISTRATION FORM VALIDATION
// ==============================

const registerForm = document.getElementById("registerForm");

if (registerForm) {

    registerForm.addEventListener("submit", function (event) {

        const username =
            document.getElementById("username").value;

        const email =
            document.getElementById("email").value;

        const password =
            document.getElementById("password").value;

        const confirmPassword =
            document.getElementById("confirmPassword").value;

        const message =
            document.getElementById("message");

        // Empty field validation
        if (
            username === "" ||
            email === "" ||
            password === "" ||
            confirmPassword === ""
        ) {
            event.preventDefault();

            message.textContent =
                "Please fill in all fields.";

            return;
        }

        // Password length validation
        if (!/^[A-Za-z0-9._-]{3,32}$/.test(username)) {
            event.preventDefault();
            message.textContent = "Username must be 3–32 characters using letters, numbers, dots, underscores, or hyphens.";
            return;
        }

        if (email.trim() === "" || email.length > 254) {
            event.preventDefault();
            message.textContent = email.trim() === ""
                ? "Email is required."
                : "Email must be no more than 254 characters.";
            return;
        }

        if (password.length < 10 || password.length > 128) {
            event.preventDefault();
            message.textContent = "Password must be 10–128 characters.";

            return;
        }

        // Confirm password validation
        if (password !== confirmPassword) {
            event.preventDefault();

            message.textContent =
                "Passwords do not match.";

            return;
        }

        // Email format validation
        const normalizedEmail = email.trim();
        const emailPattern =
            /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;
        const localPart = normalizedEmail.split("@")[0];

        if (!emailPattern.test(normalizedEmail)
                || localPart.length > 64
                || localPart.startsWith(".")
                || localPart.endsWith(".")
                || localPart.includes("..")) {
            event.preventDefault();

            message.textContent =
                "Please enter a valid email address.";

            return;
        }

        // Form is valid.
        // Do NOT use event.preventDefault() here.
        // The form will now submit to RegisterServlet.
    });

    registerForm.addEventListener("submit", async function (event) {
        if (event.defaultPrevented) return;
        event.preventDefault();
        if (registerForm.dataset.csrfPending === "true") return;
        const message = document.getElementById("message");
        const submit = registerForm.querySelector("button[type='submit']");
        registerForm.dataset.csrfPending = "true";
        submit.disabled = true;
        try {
            const token = await window.CampusCsrf.getToken();
            const body = new URLSearchParams(new FormData(registerForm));
            body.set("csrfToken", token);
            const response = await fetch(window.CampusCsrf.url(registerForm.action), {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    Accept: "application/json",
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body
            });
            if (window.CampusCsrf.isLoginRedirect(response)) {
                window.CampusCsrf.redirectToLogin();
                return;
            }
            if (response.status === 401) {
                window.CampusCsrf.redirectToLogin();
                return;
            }
            if (response.status === 403) {
                message.textContent = "Your security check expired. Reload the page and try again.";
                message.classList.add("error-message");
                return;
            }

            const contentType = response.headers.get("content-type") || "";
            const isJson = contentType.includes("application/json");
            const result = isJson ? await response.json() : {};
            if (response.ok && typeof result.redirect === "string") {
                window.location.assign(result.redirect);
                return;
            }
            message.textContent = response.ok
                ? "Registration succeeded, but the server did not provide a destination."
                : result.message || "Please review your details and try again.";
            message.classList.add("error-message");
            if (result.field && registerForm.elements[result.field]) {
                registerForm.elements[result.field].focus();
            }
        } catch (error) {
            message.textContent = "Unable to submit your registration. Please try again.";
            message.classList.add("error-message");
            console.error(error);
        } finally {
            delete registerForm.dataset.csrfPending;
            submit.disabled = false;
        }
    });
}


// ==============================
// LOGIN FORM VALIDATION
// ==============================

const loginForm = document.getElementById("loginForm");

if (loginForm) {

    loginForm.addEventListener("submit", function (event) {

        const username =
            document.getElementById("username").value.trim();

        const password =
            document.getElementById("password").value;

        const message =
            document.getElementById("loginMessage");

        // Empty field validation
        if (username === "" || password === "") {

            event.preventDefault();

            message.textContent =
                "Username and password are required.";

            return;
        }

        // Valid form
        // Do not prevent submission.
        // The form will be sent to LoginServlet.
    });
}

// ==============================
// REGISTER PASSWORD VISIBILITY
// ==============================

const passwordInput =
    document.getElementById("password");

const togglePassword =
    document.getElementById("togglePassword");

if (passwordInput && togglePassword) {

    togglePassword.addEventListener("click", function () {

        if (passwordInput.type === "password") {

            passwordInput.type = "text";
            togglePassword.textContent = "🙈";
            togglePassword.setAttribute("aria-pressed", "true");

        } else {

            passwordInput.type = "password";
            togglePassword.textContent = "👁";
            togglePassword.setAttribute("aria-pressed", "false");
        }
    });
}


// ==============================
// CONFIRM PASSWORD VISIBILITY
// ==============================

const confirmPasswordInput =
    document.getElementById("confirmPassword");

const toggleConfirmPassword =
    document.getElementById("toggleConfirmPassword");

if (confirmPasswordInput && toggleConfirmPassword) {

    toggleConfirmPassword.addEventListener("click", function () {

        if (confirmPasswordInput.type === "password") {

            confirmPasswordInput.type = "text";
            toggleConfirmPassword.textContent = "🙈";
            toggleConfirmPassword.setAttribute("aria-pressed", "true");

        } else {

            confirmPasswordInput.type = "password";
            toggleConfirmPassword.textContent = "👁";
            toggleConfirmPassword.setAttribute("aria-pressed", "false");
        }
    });
}

// Reveal landing-page sections as they enter the viewport.
const revealElements = document.querySelectorAll("[data-reveal]");

if ("IntersectionObserver" in window && revealElements.length > 0) {
    const revealObserver = new IntersectionObserver(function (entries, observer) {
        entries.forEach(function (entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add("is-visible");
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.15 });

    revealElements.forEach(function (element) {
        element.classList.add("reveal-ready");
        revealObserver.observe(element);
    });
} else {
    revealElements.forEach(function (element) {
        element.classList.add("is-visible");
    });
}

const dashboardDate = document.getElementById("dashboardDate");
const dashboardGreeting = document.getElementById("dashboardGreeting");

if (dashboardDate && dashboardGreeting) {
    const now = new Date();
    const hour = now.getHours();
    const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
    const username = dashboardGreeting.querySelector("span");

    dashboardGreeting.firstChild.textContent = greeting + ", ";
    dashboardDate.textContent = new Intl.DateTimeFormat(undefined, {
        weekday: "long",
        month: "long",
        day: "numeric"
    }).format(now);

    if (username) {
        dashboardGreeting.setAttribute("aria-label", greeting + ", " + username.textContent);
    }
}

const noticeDismiss = document.querySelector(".notice-dismiss");
const dashboardNotice = document.querySelector(".dashboard-notice");
const dashboardNoticeMessage = document.getElementById("dashboardNoticeMessage");

if (dashboardNotice) {
    const params = new URLSearchParams(window.location.search);
    const lostItemAdded = params.get("itemAdded") === "success";
    const foundItemAdded = params.get("foundAdded") === "success";

    if (lostItemAdded || foundItemAdded) {
        if (dashboardNoticeMessage) {
            dashboardNoticeMessage.textContent = foundItemAdded
                ? "Your found-item report has been submitted."
                : "Your lost-item report has been submitted.";
        }
        dashboardNotice.hidden = false;
    }
}

if (noticeDismiss) {
    noticeDismiss.addEventListener("click", function () {
        const notice = noticeDismiss.closest(".dashboard-notice");

        if (notice) {
            notice.remove();
        }
    });
}