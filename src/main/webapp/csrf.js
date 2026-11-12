(function () {
    "use strict";

    // Avoid installing duplicate handlers.
    if (window.CampusCsrf) return;

    // Resolve endpoints relative to this script's application folder.
    const appBase = new URL(
        ".",
        document.currentScript
            ? document.currentScript.src
            : document.baseURI
    );

    let tokenRequest = null;
    const pendingForms = new WeakSet();

    function url(path) {
        return new URL(path, appBase).toString();
    }

    async function requestToken() {
        const response = await fetch(url("csrf-token"), {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store",
            headers: {
                Accept: "application/json"
            }
        });

        if (!response.ok || response.redirected) {
            throw new Error("Security verification is unavailable.");
        }

        const result = await response.json();

        if (
            typeof result.token !== "string" ||
            result.token.trim() === ""
        ) {
            throw new Error("The server returned an invalid token.");
        }

        return result.token;
    }

    function getToken() {
        // Share only an ongoing request; do not cache completed tokens.
        if (!tokenRequest) {
            tokenRequest = requestToken().finally(function () {
                tokenRequest = null;
            });
        }

        return tokenRequest;
    }

    function redirectToLogin() {
        const login = new URL("login.html", appBase);
        login.searchParams.set("error", "session-expired");
        window.location.assign(login.toString());
    }

    function isLoginRedirect(response) {
        return Boolean(
            response.redirected &&
            response.url &&
            new URL(response.url).pathname ===
                new URL("login.html", appBase).pathname
        );
    }

    function showFormError(form, text) {
        let message =
            form.querySelector("[role='status']") ||
            document.getElementById("loginMessage") ||
            document.getElementById("message");

        if (!message) {
            message = document.createElement("p");
            message.setAttribute("role", "status");
            message.setAttribute("aria-live", "polite");
            form.prepend(message);
        }

        message.textContent = text;
        message.classList.remove("success-message");
        message.classList.add("error-message");
    }

    window.CampusCsrf = Object.freeze({
        getToken,
        url,
        redirectToLogin,
        isLoginRedirect
    });

    // Delegation also handles forms created after this script loads.
    // Existing form-level validation and custom handlers run first.
    document.addEventListener("submit", async function (event) {
        const form = event.target;

        if (!(form instanceof HTMLFormElement)) return;
        if (event.defaultPrevented) return;

        const submitter = event.submitter;
        const method =
            submitter && submitter.hasAttribute("formmethod")
                ? submitter.formMethod
                : form.method;

        if (method.toUpperCase() !== "POST") return;

        // Stop browser navigation immediately, before any await.
        event.preventDefault();

        if (
            pendingForms.has(form) ||
            form.dataset.csrfPending === "true"
        ) {
            return;
        }

        const action = new URL(
            submitter && submitter.hasAttribute("formaction")
                ? submitter.formAction
                : form.action,
            document.baseURI
        );

        if (action.origin !== window.location.origin) {
            showFormError(
                form,
                "This form cannot be submitted to a different site."
            );
            return;
        }

        pendingForms.add(form);
        form.dataset.csrfPending = "true";

        const buttons = Array.from(form.elements).filter(function (el) {
            return (
                (el instanceof HTMLButtonElement ||
                    el instanceof HTMLInputElement) &&
                el.type === "submit"
            );
        });

        const buttonStates = buttons.map(function (button) {
            return button.disabled;
        });

        let navigating = false;

        try {
            // Capture values before disabling the submit buttons.
            const body = new URLSearchParams(new FormData(form));

            if (submitter && submitter.name) {
                body.append(submitter.name, submitter.value);
            }

            buttons.forEach(function (button) {
                button.disabled = true;
            });

            const token = await getToken();
            body.set("csrfToken", token);

            const response = await fetch(action.toString(), {
                method: "POST",
                credentials: "same-origin",
                cache: "no-store",
                headers: {
                    Accept: "text/html",
                    "Content-Type":
                        "application/x-www-form-urlencoded;charset=UTF-8",
                    "X-CSRF-Token": token
                },
                body
            });

            // Check errors before following the final redirect URL.
            if (response.status === 403) {
                showFormError(
                    form,
                    "The server could not verify or authorize this " +
                    "request. Please try again. If it repeats, reload " +
                    "the login page."
                );
                return;
            }

            if (response.status === 401) {
                redirectToLogin();
                navigating = true;
                return;
            }

            if (!response.ok) {
                showFormError(
                    form,
                    "The request failed (HTTP " +
                    response.status +
                    "). Please try again."
                );
                return;
            }

            if (response.redirected) {
                const destination = new URL(response.url);

                if (destination.origin !== window.location.origin) {
                    throw new Error("Unexpected redirect destination.");
                }

                window.location.assign(destination.toString());
                navigating = true;
                return;
            }

            showFormError(
                form,
                "The server responded without a destination page. " +
                "Please check the server configuration."
            );
        } catch (error) {
            showFormError(
                form,
                "Unable to complete the request. Check your connection " +
                "and try again."
            );
        } finally {
            if (!navigating) {
                pendingForms.delete(form);
                delete form.dataset.csrfPending;

                buttons.forEach(function (button, index) {
                    button.disabled = buttonStates[index];
                });
            }
        }
    });
})();