---
---

/*
 * This script performs the search functionality of the site.
 * It should only be executed on the actual search page, as executing this
 * will load the entire site's text content into memory.
 */

var index = lunr(function () {
  this.field('title')
  this.field('content', { boost: 10 })
  this.field('category')
  this.field('tags')
  this.ref('id')
});

{% assign count = 0 %}
{% for post in site.pages %}
  {% if post.name contains '.md' %}
    index.add({
      title: {{post.title | jsonify}},
      category: {{post.categories[0] | jsonify}},
      content: {{post.content | strip_html | jsonify}},
      tags: {{post.tags | jsonify}},
      id: {{count}}
    });
    {% assign count = count | plus: 1 %}
    {% endif %}
{% endfor %}

var store = [{% for post in site.pages %}
  {% if post.name contains '.md' %}
    {
      "title": {{post.title | jsonify}},
      "excerpt": {{post.excerpt | truncate: 220 | jsonify}},
      "link": {{ post.url | jsonify }},
      "image": {{ post.image | jsonify }},
      "date": {{ post.date | date: '%B %-d, %Y' | jsonify }},
      "category": {{ post.categories[0] | jsonify }}
    }{% unless forloop.last %},{% endunless %}
  {% endif %}
{% endfor %}]

$(document).ready(() => {
  const urlParams = new URLSearchParams(window.location.search);
  const query = urlParams.get('term');

  if (query) {
    // Fill the query value back into the search bar
    $('#search-input').val(query);

    // Perform the search using the query param
    performSearch(query);
  }
});

function performSearch(query){
  const results = index.search(query);
  const resultsDiv = $('#results');
  resultsDiv.empty();

  if (results && results.length !== 0) {
    // Populate the search results list
    const itemMapperFunction = (item) => {
      const storeItem = store[item.ref];
      return `<li class="result-item"><h3><a href="{{ '' | relative_url }}${storeItem.link}" class="post-title">${storeItem.title}</a></h3>` +
             `<p class="markdown-body">${storeItem.excerpt || ''}</p></li>`;
    };
    resultsDiv.append(`<h1>Search results for <b>${query}</b></h1><div class="result"><ul>${results.map(itemMapperFunction).join('')}</ul></div>`);
  } else {
    // Indicate that no results were found
    resultsDiv.prepend(`<h1>No results for <b>${query}</b></h1>`);
  }
}
