---

---
var index = lunr(function () {
  this.field('title')
  this.field('content', {boost: 10})
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
    "desc": {{post.excerpt | truncate: 220 | jsonify}},
    "link": {{ post.url | jsonify }},
    "image": {{ post.image | jsonify }},
    "date": {{ post.date | date: '%B %-d, %Y' | jsonify }},
    "category": {{ post.categories[0] | jsonify }},
    "excerpt": {{ post.content | strip_html | truncatewords: 20 | jsonify }}
    }
    {% unless forloop.last %},{% endunless %}
    {% endif %}
  {% endfor %}]

$(document).ready(function() {
  var input = document.getElementById("search-input");
  input.addEventListener("keyup", function(event) {
    event.preventDefault();
    if (event.keyCode === 13) {
      var query = document.getElementById("search-input").value;
      window.location.href="/rest.li/search?term="+query;
    }
  });
});
