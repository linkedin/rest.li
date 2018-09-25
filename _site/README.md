# Rest.li github pages 

## Install 

 - Install jekyll: https://jekyllrb.com/docs/installation/
 - Get the code: `git clone`
 - Go to the `docs` directory
 - Run `jekyll serve`
 - Open a browser `http://127.0.0.1:4000`

## Contribute

Create a local branch:

```
git clone 
git checkout -b [your_branch]
```

Once you are done with your changes, you can push them:

```
git add -A
git commit -m 'your comment'
git push origin [your_branch]
```

Create a PullRequest

### How to

The documentation should use the markdown syntax and be in a `.md` file.
Each documentation page should contain a `page front matter` to specify which navigation menu to display on the left. More info [here](https://jekyllrb.com/tutorials/navigation/#scenario-5-using-a-page-variable-to-select-the-yaml-list)
The navigation menus use YAML files to generate navigation items. More info
[here](https://jekyllrb.com/tutorials/navigation/)

Jekyll tutorial: https://jekyllrb.com/tutorials/home/
