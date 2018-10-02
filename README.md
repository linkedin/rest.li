# Rest.li GitHub Pages 

## Install 

 - Install jekyll: https://jekyllrb.com/docs/installation/
 - Get the code: `git clone`
 - Check out the `gh-pages` branch:
   - `git fetch origin`
   - `git checkout --track origin/gh-pages`
 - Run `jekyll serve -b /rest.li`
 - Open a browser `http://127.0.0.1:4000/rest.li`

## Contribute

Create a local branch off the `gh-pages` branch:

```
git checkout gh-pages
git checkout -b [your_branch]
```

Once you are done with your changes, you can push them:

```
git add -A
git commit -m 'your comment'
git push origin [your_branch]
```

Create a pull request from `[your_branch]` into `gh-pages` using the ["New pull request"](https://github.com/linkedin/rest.li/compare/gh-pages...) tool on GitHub.
Have someone review, approve, and merge your PR into `gh-pages`. Clean up by deleting your branch on GitHub, unless whoever merged your branch did so already.

Now you're done, and the documentation change should be live!

### How to

The documentation should use the markdown syntax and be in a `.md` file.
Each documentation page should contain a `page front matter` to specify which navigation menu to display on the left. More info [here](https://jekyllrb.com/tutorials/navigation/#scenario-5-using-a-page-variable-to-select-the-yaml-list)
The navigation menus use YAML files to generate navigation items. More info
[here](https://jekyllrb.com/tutorials/navigation/)

Jekyll tutorial: https://jekyllrb.com/tutorials/home/

