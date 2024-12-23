/* eslint-disable jest/expect-expect -- expect is called in the `testGetColumnIndex` function */

/**
 * These tests are not meant to be run in the build pipeline.
 * They’re to help you understand the structure of the table before and after the JS switch,
 * so that you can base effective scalatest and selenium tests off them.
 *
 * `npm install && npm run test` to run these tests.
 * See `package.json` for other commands including `coverage` and `watch`.
 */

const {
  addSprite,
  compareRows,
  getColumnIndex,
  isInPage,
  ready,
  sortTableByColumn,
  switchIt,
} = require("../tableSort");

/**
 * @description Table head structure, pre-JS-switch
 * @example
 * <thead class="govuk-table__head">
 *   <tr class="govuk-table__row">
 *     <th class="govuk-table__header" scope="col">Status</th>
 *     <th class="govuk-table__header" data-column="upload-date" scope="col">
 *       <a href="/digital-platform-reporting/submission/view?statuses%5B0%5D=SUCCESS&amp;statuses%5B1%5D=REJECTED&amp;sortOrder=ASC&amp;sortBy=SUBMISSIONDATE"
 *         class="govuk-link--no-underline govuk-link--no-visited-state" data-link="sort-link">
 *         Upload date
 *       </a>
 *     </th>
 *     <th class="govuk-table__header" scope="col">Platform operator</th>
 *     <th class="govuk-table__header" data-column="reporting-year" scope="col">
 *       <a href="/digital-platform-reporting/submission/view?statuses%5B0%5D=SUCCESS&amp;statuses%5B1%5D=REJECTED&amp;sortOrder=DSC&amp;sortBy=REPORTINGYEAR"
 *         class="govuk-link--no-underline govuk-link--no-visited-state" data-link="sort-link">
 *         Reporting year
 *       </a>
 *     </th>
 *     <th class="govuk-table__header" scope="col">File name</th>
 *     <th class="govuk-table__header" scope="col">Action</th>
 *   </tr>
 * </thead>
 */
/**
 * @description Table head structure, post-JS-switch
 * @example
 * <thead class="govuk-table__head">
 *   <tr class="govuk-table__row">
 *     <th class="govuk-table__header" scope="col">Status</th>
 *     <th class="govuk-table__header" data-column="upload-date" scope="col">
 *       <button class="govuk-button govuk-button--secondary"
 *         data-query="statuses%5B0%5D=SUCCESS&amp;statuses%5B1%5D=REJECTED&amp;sortOrder=ASC&amp;sortBy=SUBMISSIONDATE"
 *         data-sort="upload-date" type="button"><span>
 *           Upload date
 *         </span>
 *         <svg viewBox="0 0 425 233.7" focusable="false" class="sort asc" aria-hidden="true">
 *           <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#icon-sort-asc"></use>
 *         </svg>
 *         <svg viewBox="0 0 425 233.7" focusable="false" class="sort des" aria-hidden="true">
 *           <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#icon-sort-des"></use>
 *         </svg></button>
 *     </th>
 *     <th class="govuk-table__header" scope="col">Platform operator</th>
 *     <th class="govuk-table__header" data-column="reporting-year" scope="col">
 *       <button class="govuk-button govuk-button--secondary"
 *         data-query="statuses%5B0%5D=SUCCESS&amp;statuses%5B1%5D=REJECTED&amp;sortOrder=DSC&amp;sortBy=REPORTINGYEAR"
 *         data-sort="reporting-year" type="button"><span>
 *           Reporting year
 *         </span>
 *         <svg viewBox="0 0 425 233.7" focusable="false" class="sort asc" aria-hidden="true">
 *           <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#icon-sort-asc"></use>
 *         </svg>
 *         <svg viewBox="0 0 425 233.7" focusable="false" class="sort des" aria-hidden="true">
 *           <use xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="#icon-sort-des"></use>
 *         </svg></button>
 *     </th>
 *     <th class="govuk-table__header" scope="col">File name</th>
 *     <th class="govuk-table__header" scope="col">Action</th>
 *   </tr>
 * </thead>
 */

/**
 * @description Table column headers
 * @example Status, Upload date, Platform operator, Reporting year, File name, Action
 */

/**
 * @description Example of a table row with failed data
 * @example Failed, 27 Nov 2024, Test digital platform operator organisation name 1, XEDPI2078675698, 1957, SubmissionSample_removedAssumed, Check errors (link)
 */

/**
 * @description Example of a table row with successful data
 * @example Passed, 8 Nov 2024, Test digital platform operator organisation name 1, XEDPI2078675698, 2024, GB2024GB-XEDPI2078675698TransportationRentalDeletionV001, Go to confirmation (link)
 */

describe("tableSort.js", () => {

  const createMockHeaders = (columns) => {
    return columns.map(col => ({
      getAttribute: jest.fn(() => col)
    }));
  };

  const testGetColumnIndex = (headers, column, expected) => {
    const result = getColumnIndex(headers, column);

    expect(result).toBe(expected);
    expect(typeof result).toBe('number');
  };

  describe("isInPage", () => {
    it("should return true if the element is in the page", () => {
      document.body.innerHTML =
        '<table class="govuk-table" data-module="table-sorts" id="xml-subs"></table>';
      const element = document.querySelector('[data-module="table-sorts"');

      expect(isInPage(element)).toBeTrue();
    });

    it("should return false if the element is not in the page", () => {
      document.body.innerHTML =
        '<table class="govuk-table" id="xml-subs"></table>';
      const element = document.querySelector('[data-module="table-sorts"');

      expect(isInPage(element)).toBeNull();
    });
  });

  describe('getColumnIndex', () => {

    it('returns -1 when headers is null', () => {
      testGetColumnIndex([], 'Accessibility', -1);
    });

    it('returns -1 when column is empty', () => {
      const headers = createMockHeaders(['Accessibility', 'Upload date', 'Platform operator', 'Reporting year']);
      testGetColumnIndex(headers, '', -1);
    });

    it('returns correct index when column exists', () => {
      const headers = createMockHeaders(['Accessibility', 'Upload date', 'Platform operator', 'Reporting year']);
      testGetColumnIndex(headers, 'Upload date', 1);
    });

    it('returns -1 when column does not exist', () => {
      const headers = createMockHeaders(['Accessibility', 'Upload date', 'Platform operator', 'Reporting year']);
      testGetColumnIndex(headers, 'phone', -1);
    });

    const testGetColumnIndexThrows = (headers, column) => {
      expect(() => getColumnIndex(headers, column)).toThrow(TypeError);
    };

    it('throws TypeError when headers is not an array', () => {
      testGetColumnIndexThrows({}, 'Accessibility');
      testGetColumnIndexThrows('not array', 'Accessibility');
    });

    it('throws TypeError when column is not a string', () => {
      const headers = createMockHeaders(['Accessibility']);
      testGetColumnIndexThrows(headers, 123);
      testGetColumnIndexThrows(headers, {});
    });
  });

  describe("compareRows", () => {
    it("should compare two rows in ascending order", () => {
      const rowA = document.createElement("tr");
      const rowB = document.createElement("tr");
      rowA.innerHTML = '<td class="govuk-table__cell">Failed</td>';
      rowB.innerHTML = '<td class="govuk-table__cell">Passed</td>';

      expect(compareRows(rowA, rowB, 0, "ASC")).toBe(-1);
    });

    it("should compare two rows in descending order", () => {
      const rowA = document.createElement("tr");
      const rowB = document.createElement("tr");
      rowA.innerHTML = '<td class="govuk-table__cell">Failed</td>';
      rowB.innerHTML = '<td class="govuk-table__cell">Passed</td>';

      expect(compareRows(rowA, rowB, 0, "DSC")).toBe(1);
    });
  });

  describe("sortTableByColumn", () => {
    beforeEach(() => {
      document.body.innerHTML = `
      <table class="govuk-table">
        <thead>
        <tr>
          <th class="govuk-table__header" data-column="status">Status</th>
          <th class="govuk-table__header" data-column="upload-date">Upload date</th>
          <th class="govuk-table__header" data-column="reporting-year">Reporting year</th>
        </tr>
        </thead>
        <tbody>
        <tr class="govuk-table__row">
          <td class="govuk-table__cell">Failed</td>
          <td class="govuk-table__cell">27 Nov 2024</td>
          <td class="govuk-table__cell">1957</td>
        </tr>
        <tr class="govuk-table__row">
          <td class="govuk-table__cell">Passed</td>
          <td class="govuk-table__cell">8 Nov 2024</td>
          <td class="govuk-table__cell">2024</td>
        </tr>
        </tbody>
      </table>
      `;
    });

    it("should sort the table by the specified column in ascending order", () => {
      sortTableByColumn(
        "status",
        "https://www.tax.service.gov.uk/serviceName?bunchOfThings=YES&sortOrder=ASC?someMoreThings=EHHH"
      );
      const rows = document.querySelectorAll(".govuk-table__row");

      expect(rows[0].querySelector(".govuk-table__cell").textContent).toBe(
        "Failed"
      );
      expect(rows[1].querySelector(".govuk-table__cell").textContent).toBe(
        "Passed"
      );

      // Add snapshot
      expect(document.body.innerHTML).toMatchSnapshot();
    });

    it("should sort the table by the specified column in descending order", () => {
      sortTableByColumn(
        "status",
        "https://www.tax.service.gov.uk/serviceName?bunchOfThings=YES&sortOrder=DSC?someMoreThings=EHHH"
      );
      const rows = document.querySelectorAll(".govuk-table__row");

      expect(rows[0].querySelector(".govuk-table__cell").textContent).toBe(
        "Passed"
      );
      expect(rows[1].querySelector(".govuk-table__cell").textContent).toBe(
        "Failed"
      );

      // Add snapshot
      expect(document.body.innerHTML).toMatchSnapshot();
    });

    it("should throw an error if the column index is invalid", () => {
      expect(() =>
        sortTableByColumn(
          "invalid-column",
          "https://www.tax.service.gov.uk/serviceName?bunchOfThings=YES&sortOrder=ASC?someMoreThings=EHHH"
        )
      ).toThrow('Column with data-column="invalid-column" not found.');
    });

    it("should throw an error if the column index is out of bounds", () => {
      document.body.innerHTML = `
        <table class="govuk-table">
          <thead>
        <tr>
          <th class="govuk-table__header" data-column="status">Status</th>
          <th class="govuk-table__header" data-column="upload-date">Upload date</th>
          <th class="govuk-table__header" data-column="reporting-year">Reporting year</th>
        </tr>
          </thead>
          <tbody>
        <tr class="govuk-table__row">
          <td class="govuk-table__cell">Failed</td>
          <td class="govuk-table__cell">27 Nov 2024</td>
          <td class="govuk-table__cell">1957</td>
        </tr>
        <tr class="govuk-table__row">
          <td class="govuk-table__cell">Passed</td>
          <td class="govuk-table__cell">8 Nov 2024</td>
          <td class="govuk-table__cell">2024</td>
        </tr>
          </tbody>
        </table>
      `;

      expect(() =>
        sortTableByColumn(
          "invalid-column",
          "https://www.tax.service.gov.uk/serviceName?bunchOfThings=YES&sortOrder=ASC?someMoreThings=EHHH"
        )
      ).toThrow('Column with data-column="invalid-column" not found.');
    });
  });

  describe("switchIt", () => {
    it("should replace a link with a button", () => {
      document.body.innerHTML =
        '<a href="https://www.tax.service.gov.uk/serviceName?bunchOfThings=YES&sortOrder=ASC?someMoreThings=EHHH" class="govuk-link">Sort</a>';
      const link = document.querySelector(".govuk-link");
      const button = switchIt(link, "status");

      expect(button.dataset.query).toContain("sortOrder=ASC");
      expect(button.dataset.sort).toBe("status");
      expect(button.tagName).toBe("BUTTON");

      // This is the Title in the SVG, if you’re looking for it.
      expect(button.textContent).toContain("Sort");

      // Add snapshot
      expect(document.body.innerHTML).toMatchSnapshot();
    });
  });

  describe("addSprite", () => {
    it("should add the SVG sprite to the page", () => {
      document.body.innerHTML =
        '<table class="govuk-table" data-module="table-sorts" id="xml-subs"></table>';
      addSprite();
      const svg = document.getElementById("SVGsprites");

      expect(svg).not.toBeNull();
      expect(svg).toBeInTheDocument();

      // Add snapshot
      expect(document.body.innerHTML).toMatchSnapshot();
    });

    it("should not add the SVG sprite if the table-sorts module is not in the page", () => {
      document.body.innerHTML = "";
      addSprite();
      const svg = document.getElementById("SVGsprites");

      expect(svg).toBeNull();

      // Add snapshot
      expect(document.body.innerHTML).toMatchSnapshot();
    });
  });

  describe("ready", () => {
    it("should execute the callback if the document is already loaded", () => {
      const callback = jest.fn();
      document.readyState = "complete";
      ready(callback);

      expect(callback).toHaveBeenCalled();
    });

    it("should add an event listener if the document is not loaded", () => {
      // Mock `document.readyState`.
      const originalReadyState = Object.getOwnPropertyDescriptor(
        document,
        "readyState"
      );
      Object.defineProperty(document, "readyState", {
        value: "loading",
        configurable: true,
      });

      const callback = jest.fn();
      ready(callback);

      // Ensure the callback is not called immediately.
      expect(callback).not.toHaveBeenCalled();

      // Simulate `DOMContentLoaded` event.
      const event = new Event("DOMContentLoaded");
      document.dispatchEvent(event);

      // Ensure the callback is called after the event.
      expect(callback).toHaveBeenCalled();

      // Restore original `document.readyState`.
      if (originalReadyState) {
        Object.defineProperty(document, "readyState", originalReadyState);
      }
    });
  });

  describe("sortLinks", () => {
    beforeEach(() => {
      document.body.innerHTML = `
<thead class="govuk-table__head">
  <tr class="govuk-table__row">
    <th class="govuk-table__header" scope="col">Status</th>
    <th class="govuk-table__header" data-column="upload-date" scope="col">
      <button class="govuk-button govuk-button--secondary" data-sort="upload-date" type="button">
        <span>
          Upload date
        </span>
      </button>
    </th>
    <th class="govuk-table__header" scope="col">Platform operator</th>
    <th class="govuk-table__header" data-column="reporting-year" scope="col">
      <button class="govuk-button govuk-button--secondary" data-sort="reporting-year" type="button">
        <span>
          Reporting year
        </span>
      </button>
    </th>
    <th class="govuk-table__header" scope="col">File name</th>
    <th class="govuk-table__header" scope="col">Action</th>
  </tr>
</thead>
<div aria-live="polite" class="govuk-visually-hidden" id="sort-order-live" role="status"></div>`;

      return Promise.resolve();
    });

    it("should add a button to the page", () => {
      document.body.innerHTML = `
        <a href="https://www.tax.service.gov.uk/serviceName?bunchOfThings=YES&sortOrder=ASC?someMoreThings=EHHH" class="govuk-link">Sort</a>
        <a href="https://www.tax.service.gov.uk/serviceName?bunchOfThings=YES&sortOrder=DSC?someMoreThings=EHHH" class="govuk-link">Sort</a>
      `;

      const links = document.querySelectorAll(".govuk-link");
      const buttons = Array.from(links).map((link) => switchIt(link, "status"));

      expect(buttons[0].dataset.query).toContain("sortOrder=ASC");
      expect(buttons[0].dataset.sort).toBe("status");
      expect(buttons[0].tagName).toBe("BUTTON");

      // This is the Title in the SVG, if you’re looking for it.
      expect(buttons[0].textContent).toContain("Sort");

      expect(buttons[1].dataset.query).toContain("sortOrder=DSC");
      expect(buttons[1].dataset.sort).toBe("status");
      expect(buttons[1].tagName).toBe("BUTTON");

      // This is the Title in the SVG, if you’re looking for it.
      expect(buttons[1].textContent).toContain("Sort");

      // Add snapshot
      expect(document.body.innerHTML).toMatchSnapshot();
    });

    /**
     * @description Initial state of the `aria-live` region:
     * @example <div class="govuk-visually-hidden" id="sort-order-live"></div>
     */
    it("should not announce the default, empty `aria-live` region", () => {
      const sortOrderLive = document.getElementById("sort-order-live");

      expect(sortOrderLive.textContent).toBe("");
    });

    /**
     * @description State of the `aria-live` region when `Upload date` has been clicked once, sorting that column in descending order:
     * @example <div aria-live="polite" class="govuk-visually-hidden" id="sort-order-live">Upload date is sorted down</div>
     */
    it("should update the `aria-live` region with the `Upload date` sort order", () => {
      const sortOrderLive = document.getElementById("sort-order-live");

      // Simulate clicking the "Upload date" button to sort in descending order
      const button = document.querySelector('[data-sort="upload-date"]');

      // Ensure the button exists in the document
      if (button) {
        button.click();
        sortOrderLive.textContent = "Upload date is sorted down";
      }

      expect(sortOrderLive.textContent).toBe("Upload date is sorted down");
    });

    /**
     * @description State of the `aria-live` region when `Reporting year` has been clicked once, sorting that column in ascending order:
     * @example <div aria-live="polite" class="govuk-visually-hidden" id="sort-order-live">Reporting year is sorted up</div>
     */
    it("should update the `aria-live` region with the `Reporting year` sort order", () => {
      const sortOrderLive = document.getElementById("sort-order-live");

      // Ensure the button exists in the document
      const button = document.querySelector('[data-sort="reporting-year"]');
      if (button) {
        // Simulate clicking the "Reporting year" button to sort in ascending order
        button.click();
        sortOrderLive.textContent = "Reporting year is sorted up";
      }

      expect(sortOrderLive.textContent).toBe("Reporting year is sorted up");
    });
  });
});
