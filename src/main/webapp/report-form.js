(function () {
    const form = document.querySelector("form[data-csrf-managed='true']");
    if (!form) return;

    const message = document.getElementById("formMessage");
    const dateInput = form.querySelector("input[type='date']");
    if (dateInput) {
        const now = new Date();
        dateInput.max = [
            now.getFullYear(),
            String(now.getMonth() + 1).padStart(2, "0"),
            String(now.getDate()).padStart(2, "0")
        ].join("-");
    }

    function showMessage(text) {
        message.textContent = text;
        message.classList.add("error-message");
    }

    function validate() {
        const required = form.querySelectorAll("[required]");
        for (const field of required) {
            if (!field.value.trim()) {
                return { field, message: field.labels[0].textContent.trim() + " is required." };
            }
        }

        for (const field of form.querySelectorAll("[maxlength]")) {
            if (field.value.length > Number(field.maxLength)) {
                return { field, message: field.labels[0].textContent.trim() + " is too long." };
            }
        }

        const date = dateInput.value;
        if (!/^\d{4}-\d{2}-\d{2}$/.test(date)
                || Number.isNaN(Date.parse(date))
                || new Date(date).toISOString().slice(0, 10) !== date) {
            return { field: dateInput, message: "Enter a real date in YYYY-MM-DD format." };
        }
        if (date > dateInput.max) {
            return { field: dateInput, message: "Report date cannot be after today." };
        }

        const contact = form.elements.contactInfo.value.trim();
        const compactPhone = contact.replace(/[ -]/g, "");
        const emailParts = contact.split("@");
        const validEmail = emailParts.length === 2
            && /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+$/.test(emailParts[0])
            && emailParts[0].length <= 64
            && !emailParts[0].startsWith(".")
            && !emailParts[0].endsWith(".")
            && !emailParts[0].includes("..")
            && /^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/.test(emailParts[1]);
        if (!validEmail && !/^(?:\+91)?[6-9]\d{9}$/.test(compactPhone)) {
            return {
                field: form.elements.contactInfo,
                message: "Enter a valid email address or Indian 10-digit mobile number."
            };
        }
        return null;
    }

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        if (form.dataset.csrfPending === "true") return;
        message.textContent = "";
        const invalid = validate();
        if (invalid) {
            showMessage(invalid.message);
            invalid.field.focus();
            return;
        }

        const submit = form.querySelector("button[type='submit']");
        form.dataset.csrfPending = "true";
        submit.disabled = true;
        try {
            const token = await window.CampusCsrf.getToken();
            const body = new URLSearchParams(new FormData(form));
            body.set("csrfToken", token);
            const response = await fetch(window.CampusCsrf.url(form.action), {
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
                showMessage("Your security check expired. Reload the page and try again.");
                return;
            }
            if (response.ok) {
                window.location.assign(response.url);
                return;
            }
            if (response.status === 400) {
                const error = await response.json();
                showMessage(error.message || "Please correct the report details and try again.");
                if (error.field && form.elements[error.field]) form.elements[error.field].focus();
                return;
            }
            showMessage("The server could not save this report. Please try again later.");
        } catch (error) {
            showMessage("Unable to submit your report. Check your connection and try again.");
            console.error(error);
        } finally {
            delete form.dataset.csrfPending;
            submit.disabled = false;
        }
    });
})();
