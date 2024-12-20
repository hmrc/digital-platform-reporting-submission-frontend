/**
 * @description Replacement for jQuery’s `$(document).ready(function()`
 * @example ready(function () {…});
 * @function ready
 */
const ready = (callback) => {
  if (document.readyState != "loading") callback();
  else document.addEventListener("DOMContentLoaded", callback);
};

/**
 * @description Replacement for jQuery’s `$('.govuk-cat__larry').length`
 * @example isInPage(document.querySelector('.govuk-cat__larry'))
 * @function isInPage
 * @param {HTMLElement} element - The element to check.
 * @returns {boolean} - Whether the element is in the page or not.
 */
function isInPage(node) {
  return node && node !== document.body && document.body.contains(node);
}

/**
 * @description Gets the index of the column to sort by.
 * @param {string} column - The column identifier.
 * @param {Array} headers - Array of table header elements
 * @returns {number} - Index of the column, or -1 if not found
 * @throws {TypeError} - If headers is not an array or column is not a string
 */
function getColumnIndex(headers, column) {
  if (!Array.isArray(headers)) {
    throw new TypeError('headers must be an array');
  }
  if (typeof column !== 'string') {
    throw new TypeError('column must be a string');
  }
  if (!headers || !column) return -1;

  return Array.from(headers).findIndex((header) => header.getAttribute('data-column') === column);
}

/**
 * @description Compares two table rows based on the content of a specific column
 * @example tableRows.sort((a, b) => compareRows(a, b, 2, "ASC")); // Sort by third column ascending
 * @param {HTMLTableRowElement} rowA - First row to compare
 * @param {HTMLTableRowElement} rowB - Second row to compare
 * @param {number} columnIndex - Index of the column to sort by
 * @param {string} sortOrder - Sort direction ("ASC" or "DESC")
 * @returns {number} - Negative if rowA < rowB, positive if rowA > rowB, 0 if equal
 */
function compareRows(rowA, rowB, columnIndex, sortOrder) {
  // Extract text content from cells and remove whitespace
  const cellA = rowA.cells[columnIndex].textContent.trim();
  const cellB = rowB.cells[columnIndex].textContent.trim();

  // Compare strings using locale-sensitive comparison
  if (sortOrder === "ASC") {
    // Ascending order: A to Z
    return cellA.localeCompare(cellB);
  } else {
    // Descending order: Z to A
    return cellB.localeCompare(cellA);
  }
}

/**
 * @description Sorts a table by a specified column and order.
 * @param {number} column - The index of the column to sort by.
 * @param {string} href - The URL containing the sortOrder parameter.
 * @throws Will throw an error if the column index is invalid or out of bounds.
 * @example Assuming the URL is 'https://www.tax.service.gov.uk/serviceName?bunchOfThings=YES&sortOrder=ASC?someMoreThings=EHHH'
 * sortTableByColumn('upload-date', 'https://www.tax.service.gov.uk/serviceName?serviceName?bunchOfThings=YES&sortOrder=ASC?someMoreThings=EHHH');
 */
function sortTableByColumn(column, href) {
  // Extract the sortOrder from the href.
  const urlParams = new URLSearchParams(href.split("?")[1]);
  const sortOrder = urlParams.get("sortOrder");

  // Get the table elements.
  const table = document.querySelector(".govuk-table");
  const tbody = table.querySelector("tbody");
  const rows = Array.from(tbody.querySelectorAll(".govuk-table__row"));

  // Sort rows based on the column and sortOrder.
  rows.sort((rowA, rowB) => {
    const cellsA = rowA.querySelectorAll(".govuk-table__cell");
    const cellsB = rowB.querySelectorAll(".govuk-table__cell");

    // If column has a data-column attribute (such as [data-column="upload-date"]), find the index of the column as an integer, using zero-based indexing.
    // -  if the column is 'upload-date', the columnIndex will be 1.
    // -  if the column is 'reporting-year', the columnIndex will be 3.

    const headers = Array.from(
      rowA.parentNode.parentNode.querySelectorAll(".govuk-table__header")
    );

    const columnIndex = getColumnIndex(headers, column);

    if (columnIndex === -1) {
      throw new Error(`Column with data-column="${column}" not found.`);
    }

    // If the column index is invalid or out of bounds, throw an error.
    if (columnIndex < 0 || columnIndex >= headers.length) {
      throw new Error(`Column index ${columnIndex} is out of bounds.`);
    }

    // If the column is not a number, throw an error.
    if (isNaN(columnIndex)) {
      throw new Error(`Column ${column} is not a number.`);
    }

    const cellA = cellsA[columnIndex];
    const cellB = cellsB[columnIndex];

    // Check if the cells exist and have text content.
    if (
      !cellA ||
      !cellB ||
      cellA.textContent === null ||
      cellB.textContent === null
    ) {
      console.error(
        `One of the cells is undefined or has no text content. Cell A: ${cellA}, Cell B: ${cellB}`
      );
      return 0;
    }

    // Compare the text content of the cells based on the sortOrder
    return compareRows(rowA, rowB, columnIndex, sortOrder);
  });

  // Clear existing rows
  while (tbody.firstChild) {
    tbody.removeChild(tbody.firstChild);
  }

  // Append sorted rows
  rows.forEach((row) => tbody.appendChild(row));
}

/**
 * @description Replaces a link with a button.
 * @param {HTMLElement} element - The link element to be replaced.
 * @param {string} column - The column identifier.
 * @returns {HTMLButtonElement} - The created button element.
 */
function switchIt(element, column) {
  // Create a button element
  const button = document.createElement("button");
  const span = document.createElement("span");
  const svgArrows = `
    <svg viewBox="0 0 425 233.7" focusable="false" class="sort asc" aria-hidden="true">
      <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#icon-sort-asc"></use>
    </svg>
    <svg viewBox="0 0 425 233.7" focusable="false" class="sort des" aria-hidden="true">
      <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#icon-sort-des"></use>
    </svg>`;

  // Set the textContent of the span
  span.textContent = element.textContent;

  // Append the span to the button
  button.appendChild(span);

  // Add the arrows after the textContent span
  button.insertAdjacentHTML("beforeend", svgArrows);

  // Set button attributes
  button.classList.add("govuk-button", "govuk-button--secondary");
  button.dataset.query = element.href.split("?")[1];
  button.dataset.sort = column;
  button.type = "button";

  // Replace the link with the button
  element.parentNode.replaceChild(button, element);

  return button;
}

/**
 * @description Adds the SVG sprite to the page.
 */
function addSprite() {
  // append the sprite to the page, after [data-module="table-sorts"]
  const svgSprite = `
    <svg version="1.1" xmlns="http://www.w3.org/2000/svg" id="SVGsprites">
      <style>.l{fill:none;stroke-width:30;stroke-miterlimit:10;}.t{stroke:none;}</style>
      <defs>
        <g id="icon-sort" aria-labeledby="title-sort" aria-describedby="desc-sort" role="image">
          <title id="title-sort">Sort</title>
          <desc id="desc-sort"></desc>
          <path class="t" d="M20.4 233.7L212.5 41.6l192.1 192.1z"/>
          <path class="l" d="M414.4 223.1L212.5 21.2 10.6 223.1"/>
          <path class="t" d="M404.6 306L212.5 498.1 20.4 306z"/>
          <path class="l" d="M10.6 316.6l201.9 201.9 201.9-201.9"/>
        </g>
        <g id="icon-sort-asc" aria-labeledby="" aria-describedby="" role="presentation">
          <title id="title-sort-asc"></title>
          <desc id="desc-sort-asc"></desc>
          <path class="t" d="M20.4 233.7L212.5 41.6l192.1 192.1z"/>
          <path class="l" d="M414.4 223.1L212.5 21.2 10.6 223.1"/>
        </g>
        <g id="icon-sort-des" aria-labeledby="" aria-describedby="" role="presentation">
          <title id="title-sort-des"></title>
          <desc id="desc-sort-des"></desc>
          <path class="t" d="M404.6 0L212.5 192.1 20.4 0z"/>
          <path class="l" d="M10.6 10.6l201.9 201.9L414.4 10.6"/>
        </g>
      </defs>
    </svg>`;

  const tableSorts = document.querySelector('[data-module="table-sorts"]');
  if (!isInPage(tableSorts)) return;

  tableSorts.insertAdjacentHTML("afterend", svgSprite);
}

ready(async () => {
  // If the tableSort module is not in the page, exit.
  if (!isInPage(document.querySelector('[data-module="table-sorts"]'))) return;

  addSprite();

  const sortLinks = document.querySelectorAll('[data-link="sort-link"]');

  sortLinks.forEach((link) => {
    const th = link.closest(".govuk-table__header");
    if (!th) {
      console.error(
        "Could not find the closest .govuk-table__header for the link."
      );
      return;
    }

    // Not every column header has a data-column attribute.
    const column = th.dataset.column;
    if (!column) {
      console.error("Column header does not have a data-column attribute.");
      return;
    }

    // We’ve found a column, so replace the link with a button.
    const button = switchIt(link, column);

    // Add event listener for sorting.
    button.addEventListener("click", (event) => {
      event.preventDefault();

      // Toggle between `sortOrder=ASC` and `sortOrder=DSC` within `buttonQuery`.
      let query = button.dataset.query;
      const sortOrder = query.includes("sortOrder=ASC") ? "DSC" : "ASC";
      query = query.replace(/sortOrder=(ASC|DSC)/, `sortOrder=${sortOrder}`);
      button.dataset.query = query;

      // When `sortOrder` is `ASC`, set `aria-sort` to ascending on `column`, otherwise set it to descending.
      th.setAttribute(
        "aria-sort",
        sortOrder === "ASC" ? "ascending" : "descending"
      );

      // Unset the `data-sorted` attribute on all cells in the table.
      const allCells = document.querySelectorAll(
        ".govuk-table__cell, .govuk-table__header"
      );
      allCells.forEach((cell) => {
        cell.removeAttribute("data-sorted");
      });

      // Set the `data-sorted` attribute to the `sortOrder` on the `td` of each cell in the column.
      const td = Array.from(th.parentNode.children).indexOf(th);
      const rows = Array.from(document.querySelectorAll(".govuk-table__row"));
      rows.forEach((row) => {
        const cell = row.children[td];
        cell.dataset.sorted = sortOrder;
      });

      // Call the function to sort the table by the specified column.
      const baseUrl = window.location.origin + window.location.pathname;
      const fullUrl = `${baseUrl}?${query}`;
      sortTableByColumn(column, fullUrl);

      // Update the `aria-live` region with the sort order only if it exists and is not empty.
      // We are using an `output` element here, which gives us `aria-live` for free.
      // TODO Improve this to be more friendly to the messages file.
      const sortOrderLive = document.getElementById("sort-order-live");
      if (!sortOrderLive) {
        console.error("Could not find the sort-order-live element.");
        return;
      }

      if (sortOrderLive) {
        const columnText = button.querySelector("span").textContent.trim();
        const newText = `${columnText} is sorted ${
          sortOrder === "ASC" ? "up" : "down"
        }`;

        if (sortOrderLive.textContent.trim() !== newText) {
          sortOrderLive.textContent = newText;
        }
      }
    });
  });
});

if (typeof exports !== "undefined") {
  exports.addSprite = addSprite;
  exports.compareRows = compareRows;
  exports.getColumnIndex = getColumnIndex;
  exports.isInPage = isInPage;
  exports.sortTableByColumn = sortTableByColumn;
  exports.switchIt = switchIt;
  exports.ready = ready;
}
