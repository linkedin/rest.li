/*
 * This script just allows the search bar to navigate the user to the search page.
 */
$(document).ready(() => {
  const searchInput = $('#search-input');
  searchInput.keydown((event) => {
    const enterPressed = event.which === 13;
    const query = searchInput.val().trim();
    if (enterPressed && query) {
      event.preventDefault();
      window.location.href = `/rest.li/search?term=${query}`;
    }
  });
});
