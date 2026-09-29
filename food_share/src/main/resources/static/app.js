const state = {
  donors: [],
  ngos: [],
  listings: [],
  claims: [],
  impact: []
};

const elements = {
  donorSelect: document.getElementById("donor-select"),
  ngoSelect: document.getElementById("ngo-select"),
  donorForm: document.getElementById("donor-form"),
  ngoForm: document.getElementById("ngo-form"),
  listingForm: document.getElementById("listing-form"),
  listingGrid: document.getElementById("listing-grid"),
  listingCount: document.getElementById("listing-count"),
  listingSearch: document.getElementById("listing-search"),
  pickupAreaFilter: document.getElementById("pickup-area-filter"),
  donorListingRows: document.getElementById("donor-listing-rows"),
  claimList: document.getElementById("claim-list"),
  impactRows: document.getElementById("impact-rows"),
  globalStatus: document.getElementById("global-status"),
  safeUntil: document.getElementById("safe-until")
};

function setMessage(message, kind = "info") {
  elements.globalStatus.textContent = message;
  elements.globalStatus.dataset.kind = kind;
}

async function request(path, options = {}) {
  const headers = { Accept: "application/json", ...(options.body ? { "Content-Type": "application/json" } : {}), ...options.headers };
  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  let result = null;

  if (text) {
    try {
      result = JSON.parse(text);
    } catch {
      result = { message: text };
    }
  }

  if (!response.ok) {
    throw new Error(result?.message || "Request failed (HTTP " + response.status + ")");
  }

  return result;
}

function makeElement(tagName, className, text) {
  const element = document.createElement(tagName);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}

function makeEmpty(container, message) {
  container.replaceChildren(makeElement("p", "empty-state", message));
}

function addTableCell(row, value) {
  const cell = document.createElement("td");
  cell.textContent = value;
  row.appendChild(cell);
}

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function formatQuantity(quantity, unit) {
  return Number(quantity).toLocaleString(undefined, { maximumFractionDigits: 3 }) + " " + unit;
}

function fillProfileSelect(select, profiles, placeholder, preferredId) {
  const emptyOption = document.createElement("option");
  emptyOption.value = "";
  emptyOption.textContent = placeholder;
  select.replaceChildren(emptyOption);

  for (const profile of profiles) {
    const option = document.createElement("option");
    option.value = String(profile.id);
    option.textContent = profile.name;
    select.appendChild(option);
  }

  const hasPreferred = profiles.some((profile) => String(profile.id) === String(preferredId));
  select.value = hasPreferred ? String(preferredId) : "";
  if (!select.value && profiles.length > 0) select.value = String(profiles[0].id);
}

async function loadProfiles(preferred = {}) {
  const currentDonor = preferred.donorId ?? elements.donorSelect.value;
  const currentNgo = preferred.ngoId ?? elements.ngoSelect.value;
  const [donors, ngos] = await Promise.all([request("/api/donors"), request("/api/ngos")]);
  state.donors = donors;
  state.ngos = ngos;

  fillProfileSelect(elements.donorSelect, donors,
    donors.length ? "Select a donor" : "Register a donor first", currentDonor);
  fillProfileSelect(elements.ngoSelect, ngos,
    ngos.length ? "Select an NGO" : "Register an NGO first", currentNgo);

  document.getElementById("claim-guidance").hidden = Boolean(elements.ngoSelect.value);
  await Promise.all([loadDonorListings(), loadClaims()]);
  renderListings();
}

function renderListings() {
  const query = elements.listingSearch.value.trim().toLocaleLowerCase();
  const visible = state.listings.filter((listing) =>
    (listing.name + " " + listing.foodType + " " + (listing.pickupArea || "")).toLocaleLowerCase().includes(query));
  elements.listingGrid.replaceChildren();

  if (visible.length === 0) {
    makeEmpty(elements.listingGrid, state.listings.length ? "No listings match this search." : "No available food at the moment.");
  } else {
    for (const listing of visible) {
      const card = makeElement("article", "listing-card");
      const name = makeElement("h3", "", listing.name);
      const type = makeElement("span", "listing-type", listing.foodType);
      const donor = state.donors.find((profile) => profile.id === listing.donorId);
      const details = makeElement("p", "listing-details",
        formatQuantity(listing.quantity, listing.unit) + " · " + (listing.pickupArea || "Area not specified")
          + " · Safe until " + formatDate(listing.safeUntil));
      const contributor = makeElement("p", "", "Offered by " + (donor?.name || "Community donor"));
      const claimButton = makeElement("button", "button button-green", "Claim food");
      claimButton.type = "button";
      claimButton.disabled = !elements.ngoSelect.value;
      claimButton.setAttribute("aria-label", "Claim " + listing.name);
      claimButton.dataset.claimId = String(listing.id);
      card.append(name, type, details, contributor, claimButton);
      elements.listingGrid.appendChild(card);
    }
  }

  elements.listingCount.textContent = visible.length + (visible.length === 1 ? " listing available" : " listings available");
  elements.listingGrid.setAttribute("aria-busy", "false");
}

async function loadListings() {
  elements.listingGrid.setAttribute("aria-busy", "true");
  elements.listingCount.textContent = "Loading listings...";
  try {
    const area = elements.pickupAreaFilter.value.trim();
    const areaQuery = area ? "?area=" + encodeURIComponent(area) : "";
    state.listings = await request("/api/listings" + areaQuery);
    renderListings();
  } catch (error) {
    makeEmpty(elements.listingGrid, "Listings could not be loaded. Check the connection and try again.");
    elements.listingCount.textContent = "Listings unavailable";
    elements.listingGrid.setAttribute("aria-busy", "false");
    setMessage(error.message, "error");
  }
}

async function loadDonorListings() {
  const donorId = elements.donorSelect.value;
  elements.donorListingRows.replaceChildren();
  if (!donorId) {
    addEmptyTableRow(elements.donorListingRows, 5, "Register or select a donor to view listings.");
    return;
  }

  try {
    const listings = await request("/api/donors/" + encodeURIComponent(donorId) + "/listings");
    if (listings.length === 0) {
      addEmptyTableRow(elements.donorListingRows, 5, "This donor has not published food yet.");
      return;
    }

    for (const listing of listings) {
      const row = document.createElement("tr");
      addTableCell(row, listing.name + " · " + listing.foodType);
      addTableCell(row, formatQuantity(listing.quantity, listing.unit));
      addTableCell(row, listing.pickupArea || "Not specified");
      addTableCell(row, formatDate(listing.safeUntil));
      const status = makeElement("span", "status-label status-" + listing.status.toLowerCase(), listing.status);
      const statusCell = document.createElement("td");
      statusCell.appendChild(status);
      row.appendChild(statusCell);
      elements.donorListingRows.appendChild(row);
    }
  } catch (error) {
    addEmptyTableRow(elements.donorListingRows, 5, "Could not load this donor's listings.");
    setMessage(error.message, "error");
  }
}

async function loadClaims() {
  const ngoId = elements.ngoSelect.value;
  elements.claimList.setAttribute("aria-busy", "true");
  if (!ngoId) {
    state.claims = [];
    makeEmpty(elements.claimList, "Register or select an NGO to view its claims.");
    elements.claimList.setAttribute("aria-busy", "false");
    return;
  }

  try {
    state.claims = await request("/api/ngos/" + encodeURIComponent(ngoId) + "/claims");
    renderClaims();
  } catch (error) {
    makeEmpty(elements.claimList, "Claims could not be loaded. Try refreshing.");
    elements.claimList.setAttribute("aria-busy", "false");
    setMessage(error.message, "error");
  }
}

function renderClaims() {
  elements.claimList.replaceChildren();
  if (state.claims.length === 0) {
    makeEmpty(elements.claimList, "No claims for this NGO yet.");
    elements.claimList.setAttribute("aria-busy", "false");
    return;
  }

  for (const claim of state.claims) {
    const row = makeElement("article", "claim-row");
    const description = document.createElement("div");
    description.append(
      makeElement("h3", "", claim.listingName),
      makeElement("p", "", formatQuantity(claim.quantity, claim.unit) + " · " + (claim.pickupArea || "Area not specified")
        + " · Claimed " + formatDate(claim.claimedAt))
    );
    const status = makeElement("span", "status-label status-" + claim.status.toLowerCase(), claim.status);
    row.append(description, status);

    if (claim.status === "CLAIMED") {
      const collectButton = makeElement("button", "button button-outline", "Mark collected");
      collectButton.type = "button";
      collectButton.dataset.collectId = String(claim.id);
      collectButton.setAttribute("aria-label", "Mark " + claim.listingName + " as collected");
      row.appendChild(collectButton);
    }

    elements.claimList.appendChild(row);
  }
  elements.claimList.setAttribute("aria-busy", "false");
}

function addEmptyTableRow(body, columnCount, message) {
  const row = document.createElement("tr");
  const cell = document.createElement("td");
  cell.colSpan = columnCount;
  cell.textContent = message;
  row.appendChild(cell);
  body.appendChild(row);
}

async function loadImpact() {
  elements.impactRows.replaceChildren();
  try {
    state.impact = await request("/api/statistics/diverted/monthly");
    if (state.impact.length === 0) {
      addEmptyTableRow(elements.impactRows, 3, "Collected food totals will appear here.");
      return;
    }

    for (const item of state.impact) {
      const row = document.createElement("tr");
      addTableCell(row, item.month);
      addTableCell(row, Number(item.totalQuantity).toLocaleString(undefined, { maximumFractionDigits: 3 }));
      addTableCell(row, item.unit);
      elements.impactRows.appendChild(row);
    }
  } catch (error) {
    addEmptyTableRow(elements.impactRows, 3, "Impact totals could not be loaded.");
    setMessage(error.message, "error");
  }
}

async function registerProfile(form, endpoint, select, preferredKey, inputId, label) {
  const name = document.getElementById(inputId).value.trim();
  if (!name) {
    document.getElementById(inputId).setCustomValidity("Enter an organization name.");
    form.reportValidity();
    document.getElementById(inputId).setCustomValidity("");
    return;
  }

  const submit = form.querySelector("button[type=submit]");
  submit.disabled = true;
  try {
    const profile = await request(endpoint, { method: "POST", body: JSON.stringify({ name }) });
    form.reset();
    await loadProfiles({ [preferredKey]: profile.id, ...(preferredKey === "donorId" ? { ngoId: elements.ngoSelect.value } : { donorId: elements.donorSelect.value }) });
    setMessage(label + " added and selected.", "success");
  } catch (error) {
    setMessage(error.message, "error");
  } finally {
    submit.disabled = false;
  }
}

function localDateTimeValue(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function setDefaultExpiry() {
  const minimum = new Date(Date.now() + 60_000);
  elements.safeUntil.min = localDateTimeValue(minimum);
  elements.safeUntil.value = localDateTimeValue(new Date(Date.now() + 4 * 60 * 60_000));
}

async function publishListing(event) {
  event.preventDefault();
  if (!elements.donorSelect.value) {
    setMessage("Register or select a donor before publishing food.", "error");
    document.getElementById("organizations").scrollIntoView({ behavior: "smooth" });
    return;
  }

  const submit = document.getElementById("publish-listing");
  submit.disabled = true;
  const requestBody = {
    donorId: Number(elements.donorSelect.value),
    name: document.getElementById("food-name").value.trim(),
    foodType: document.getElementById("food-type").value.trim(),
    quantity: Number(document.getElementById("food-quantity").value),
    unit: document.getElementById("food-unit").value.trim(),
    pickupArea: document.getElementById("pickup-area").value.trim(),
    safeUntil: elements.safeUntil.value
  };

  try {
    await request("/api/listings", { method: "POST", body: JSON.stringify(requestBody) });
    elements.listingForm.reset();
    setDefaultExpiry();
    await Promise.all([loadListings(), loadDonorListings()]);
    setMessage("Food listing published.", "success");
    document.getElementById("available").scrollIntoView({ behavior: "smooth" });
  } catch (error) {
    setMessage(error.message, "error");
  } finally {
    submit.disabled = false;
  }
}

async function claimListing(listingId, button) {
  if (!elements.ngoSelect.value) {
    setMessage("Register or select an NGO before claiming food.", "error");
    return;
  }

  button.disabled = true;
  try {
    await request("/api/listings/" + encodeURIComponent(listingId) + "/claims", {
      method: "POST",
      body: JSON.stringify({ ngoId: Number(elements.ngoSelect.value) })
    });
    setMessage("Food claimed for " + elements.ngoSelect.selectedOptions[0].textContent + ".", "success");
    await Promise.all([loadListings(), loadClaims()]);
    document.getElementById("claims").scrollIntoView({ behavior: "smooth" });
  } catch (error) {
    setMessage(error.message, "error");
    await loadListings();
  } finally {
    button.disabled = false;
  }
}

async function markCollected(claimId, button) {
  button.disabled = true;
  try {
    await request("/api/claims/" + encodeURIComponent(claimId) + "/collected", { method: "PATCH" });
    setMessage("Pickup confirmed. Monthly impact has been updated.", "success");
    await Promise.all([loadClaims(), loadListings(), loadDonorListings(), loadImpact()]);
  } catch (error) {
    setMessage(error.message, "error");
    await loadClaims();
  } finally {
    button.disabled = false;
  }
}

function bindEvents() {
  elements.donorForm.addEventListener("submit", (event) => {
    event.preventDefault();
    registerProfile(elements.donorForm, "/api/donors", elements.donorSelect, "donorId", "donor-name", "Donor");
  });
  elements.ngoForm.addEventListener("submit", (event) => {
    event.preventDefault();
    registerProfile(elements.ngoForm, "/api/ngos", elements.ngoSelect, "ngoId", "ngo-name", "NGO");
  });
  elements.listingForm.addEventListener("submit", publishListing);
  elements.donorSelect.addEventListener("change", loadDonorListings);
  elements.ngoSelect.addEventListener("change", () => {
    document.getElementById("claim-guidance").hidden = Boolean(elements.ngoSelect.value);
    renderListings();
    loadClaims();
  });
  elements.listingSearch.addEventListener("input", renderListings);
  document.getElementById("refresh-listings").addEventListener("click", loadListings);
  document.getElementById("apply-area-filter").addEventListener("click", loadListings);
  document.getElementById("refresh-claims").addEventListener("click", loadClaims);
  document.getElementById("refresh-impact").addEventListener("click", loadImpact);
  elements.listingGrid.addEventListener("click", (event) => {
    const button = event.target.closest("[data-claim-id]");
    if (button) claimListing(button.dataset.claimId, button);
  });
  elements.claimList.addEventListener("click", (event) => {
    const button = event.target.closest("[data-collect-id]");
    if (button) markCollected(button.dataset.collectId, button);
  });
}

async function initialize() {
  setDefaultExpiry();
  bindEvents();
  try {
    await loadProfiles();
  } catch (error) {
    setMessage("Organization profiles could not be loaded: " + error.message, "error");
  }
  await Promise.all([loadListings(), loadImpact()]);
}

function refreshDashboard() {
  if (document.visibilityState !== "visible") return;
  loadProfiles().catch((error) => setMessage(error.message, "error"));
  loadListings();
  loadImpact();
}

document.addEventListener("visibilitychange", refreshDashboard);
window.setInterval(refreshDashboard, 20_000);
initialize();