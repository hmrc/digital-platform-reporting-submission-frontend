// Note - updated to work with the HMRC Frontend implementation
// https://github.com/hmrc/play-frontend-hmrc#adding-accessible-autocomplete-css-and-javascript

if (typeof HMRCAccessibleAutocomplete != 'undefined' && document.querySelector('[data-module="hmrc-accessible-autocomplete"]') != null) {
    var originalSelect = document.querySelector('[data-module="hmrc-accessible-autocomplete"]');
    // load autocomplete - now handled by the HMRC component wrapper in Twirl
    // accessibleAutocomplete.enhanceSelectElement({
    //     selectElement: originalSelect,
    //     showAllValues: true
    // });

    // =====================================================
    // Polyfill autocomplete once loaded
    // =====================================================
    var checkForLoad = setInterval(checkForAutocompleteLoad, 50);
    var parentForm = upTo(originalSelect, 'form');

    function polyfillAutocomplete(){
        var combo = parentForm.querySelector('[role="combobox"]');

        // =====================================================
        // Update autocomplete once loaded with fallback's aria attributes
        // Ensures hint and error are read out before usage instructions
        // =====================================================
        if(originalSelect && originalSelect.getAttribute('aria-describedby') > ""){
            if(parentForm){
                if(combo){
                    combo.setAttribute('aria-describedby', originalSelect.getAttribute('aria-describedby') + ' ' + combo.getAttribute('aria-describedby'));
                }
            }
        }
        // =====================================================
        // Update autocomplete once loaded with error styling if needed
        // This won't work if the autocomplete css is loaded after the frontend library css because
        // the autocomplete's border will override the error class's border (they are both the same specificity)
        // but we can use the class assigned to build a more specific rule
        // =====================================================
        setErrorClass();
        function setErrorClass(){
            if(originalSelect && originalSelect.classList.contains("govuk-select--error")){
                if(parentForm){
                    if(combo){
                        combo.classList.add("govuk-input--error");
                        // Also set up an event listener to check for changes to input so we know when to repeat the copy
                        combo.addEventListener('focus', function(){setErrorClass()});
                        combo.addEventListener('blur', function(){setErrorClass()});
                        combo.addEventListener('change', function(){setErrorClass()});
                    }
                }
            }
        }

        // =====================================================
        // Ensure when user replaces valid answer with a non-valid answer, then valid answer is not retained
        // =====================================================
        var holdSubmit = true;
        parentForm.addEventListener('submit', function(e){
            if(holdSubmit){
                e.preventDefault()
                if(originalSelect.querySelectorAll('[selected]').length > 0 || originalSelect.value > ""){

                    var resetSelect = false;

                    if(originalSelect.value){
                        if(combo.value != originalSelect.querySelector('option[value="' + originalSelect.value +'"]').text){
                            resetSelect = true;
                        }
                    }
                    if(resetSelect){
                        originalSelect.value = "";
                        if(originalSelect.querySelectorAll('[selected]').length > 0){
                            originalSelect.querySelectorAll('[selected]')[0].removeAttribute('selected');
                        }
                    }
                }

                holdSubmit = false;
                //parentForm.submit();
                HTMLFormElement.prototype.submit.call(parentForm); // because submit buttons have id of "submit" which masks the form's natural form.submit() function
            }
        })

    }
    function checkForAutocompleteLoad(){
        if(parentForm.querySelector('[role="combobox"]')){
            clearInterval(checkForLoad)
            polyfillAutocomplete();
        }
    }
}

// Find first ancestor of el with tagName
// or undefined if not found
function upTo(el, tagName) {
    tagName = tagName.toLowerCase();

    while (el && el.parentNode) {
        el = el.parentNode;
        if (el.tagName && el.tagName.toLowerCase() == tagName) {
            return el;
        }
    }

    // Many DOM methods return null if they don't
    // find the element they are searching for
    // It would be OK to omit the following and just
    // return undefined
    return null;
}

// =====================================================
// This JavaScript restructures the <select> and <option> elements into a custom <ul>/<li> dropdown to handle long option text more effectively.
// =====================================================

document.querySelectorAll(".custom-select select").forEach(function (select) {
    const wrapper = select.parentElement;
    const options = Array.from(select.options);
    const selectedOption = options[select.selectedIndex];
    const listId = select.getAttribute("aria-controls") ||
        select.id + "-options";

    // Keep the field value available for form submission.
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = select.name;
    input.value = select.value;
    input.disabled = select.disabled;

    // Create the dropdown trigger.
    const button = document.createElement("button");
    button.type = "button";
    button.id = select.id;
    button.className = select.className + " custom-select-trigger";
    button.disabled = select.disabled;
    button.setAttribute("aria-expanded", "false");
    button.setAttribute("aria-controls", listId);
    button.setAttribute("aria-haspopup", "listbox");

    ["aria-describedby", "aria-invalid"].forEach(function (attribute) {
        if (select.hasAttribute(attribute)) {
            button.setAttribute(attribute, select.getAttribute(attribute));
        }
    });

    const value = document.createTextNode(
        selectedOption ? selectedOption.text : ""
    );

    button.appendChild(value);
    button.insertAdjacentHTML("beforeend", `
        <svg class="custom-select-arrow"
             width="14" height="10" viewBox="0 0 14 10"
             aria-hidden="true" focusable="false">
            <path d="M2 2 L7 7 L12 2"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"/>
        </svg>
    `);

    const list = document.createElement("ul");
    list.id = listId;
    list.className = "custom-select-options";
    list.setAttribute("role", "listbox");
    list.hidden = true;

    const label = select.labels && select.labels[0];

    if (label) {
        if (!label.id) {
            label.id = select.id + "-label";
        }

        list.setAttribute("aria-labelledby", label.id);
    } else if (select.hasAttribute("aria-label")) {
        button.setAttribute("aria-label", select.getAttribute("aria-label"));
        list.setAttribute("aria-label", select.getAttribute("aria-label"));
    }

    function enabledItems() {
        return Array.from(
            list.querySelectorAll('[role="option"][aria-disabled="false"]')
        );
    }

    function setOpen(open, focusLast) {
        list.hidden = !open;
        button.setAttribute("aria-expanded", String(open));

        if (open) {
            const items = enabledItems();
            const selected = items.find(function (item) {
                return item.getAttribute("aria-selected") === "true";
            });

            const target = selected ||
                (focusLast ? items[items.length - 1] : items[0]);

            if (target) {
                target.focus();
            }
        }
    }

    options.forEach(function (option) {
        const disabled = option.disabled ||
            (option.parentElement.tagName === "OPTGROUP" &&
                option.parentElement.disabled);

        const item = document.createElement("li");
        item.textContent = option.text;
        item.className = "autocomplete__option";
        item.tabIndex = -1;
        item.setAttribute("role", "option");
        item.setAttribute("aria-selected", String(option.selected));
        item.setAttribute("aria-disabled", String(disabled));
        item.classList.toggle("is-selected", option.selected);

        function selectOption() {
            if (disabled || button.disabled) return;

            input.value = option.value;
            value.nodeValue = option.text;

            list.querySelectorAll('[role="option"]').forEach(function (element) {
                const selected = element === item;

                element.classList.toggle("is-selected", selected);
                element.setAttribute("aria-selected", String(selected));
            });

            setOpen(false);
            button.focus();

            input.dispatchEvent(new Event("change", { bubbles: true }));
        }

        item.addEventListener("click", selectOption);

        item.addEventListener("keydown", function (event) {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                selectOption();
            }
        });

        list.appendChild(item);
    });

    button.addEventListener("click", function () {
        setOpen(list.hidden);
    });

    button.addEventListener("keydown", function (event) {
        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            setOpen(true, event.key === "ArrowUp");
        }
    });

    let searchText = "";
    let searchTimer;

    list.addEventListener("keydown", function (event) {
        const items = enabledItems();
        if (!items.length) return;

        let index = items.indexOf(document.activeElement);

        switch (event.key) {
            case "ArrowDown":
                index = Math.min(index + 1, items.length - 1);
                break;
            case "ArrowUp":
                index = Math.max(index - 1, 0);
                break;
            case "Home":
                index = 0;
                break;
            case "End":
                index = items.length - 1;
                break;
            default:
                // Type letters to focus a matching option.
                if (
                    event.key.length === 1 &&
                    event.key !== " " &&
                    !event.ctrlKey &&
                    !event.altKey &&
                    !event.metaKey
                ) {
                    event.preventDefault();
                    clearTimeout(searchTimer);
                    searchText += event.key.toLowerCase();

                    const match = items.find(function (item) {
                        return item.textContent.trim().toLowerCase()
                            .startsWith(searchText);
                    });

                    if (match) match.focus();

                    searchTimer = setTimeout(function () {
                        searchText = "";
                    }, 500);
                }
                return;
        }

        event.preventDefault();
        items[index].focus();
    });

    wrapper.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !list.hidden) {
            event.preventDefault();
            setOpen(false);
            button.focus();
        }
    });

    wrapper.addEventListener("focusout", function (event) {
        if (!wrapper.contains(event.relatedTarget)) {
            setOpen(false);
        }
    });

    document.addEventListener("click", function (event) {
        if (!wrapper.contains(event.target)) {
            setOpen(false);
        }
    });

    // Remove the original select and options.
    select.replaceWith(input, button, list);
});