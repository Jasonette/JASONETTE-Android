# How to contribute to Jasonette

## **Want to help with documentation?**

If you would like to contribute to the [documentation](https://jasonette.github.io/documentation/), let's discuss on the [documentation repository](https://github.com/Jasonette/documentation/issues).

## **Do you have a bug report or a feature request?**

* **Ensure the bug was not already reported** by searching on GitHub under [Issues](https://github.com/Jasonette/JASONETTE-Android/issues).

* If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/Jasonette/JASONETTE-Android/issues/new). Be sure to include a **title and clear description**, as much relevant information as possible, and a **code sample** or an **executable test case** demonstrating the expected behavior that is not occurring.


## **Did you write a patch that fixes a bug?**

* Open a new GitHub pull request with the patch.

* Don't fork `master` branch. **Fork `develop` branch and send a pull request to `develop`.

* Ensure the PR description clearly describes the problem and solution. Include the relevant issue number if applicable.

## **Did you write a cool extension?**

Feel free to fork the project and [write your own extension](https://jasonette.github.io/documentation/advanced)

If you wrote a cool extension, please share it with the community in the [slack channel](https://jasonette.now.sh).

## **Do you have other types of questions?**

* Ask any question about how to use Jasonette on the [Jasonette Slack channel](https://jasonette.now.sh).

## **Project Structure**

### Class hierarchy
![hierarchy](https://raw.githubusercontent.com/gliechtenstein/images/master/android_hierarchy.png)

Here's a brief walkthrough of how the project is structured:

  - java
    - com.jasonette.seed
      - **Action**: Where all [actions](https://jasonette.github.io/documentation/actions/) are implemented. The implementation follows [the convention described here](https://jasonette.github.io/documentation/advanced/#2-extend-actions).
      - **Component**: Implements [components](https://jasonette.github.io/documentation/components/), following [the convention described here](https://jasonette.github.io/documentation/advanced/#1-extend-ui-components).
      - **Core**: Core logic that handles action dispatch, view construction, templating, and some native system actions.
        - JasonModel: Deals with all the data associated with JasonViewActivity.
        - JasonParser: Handles template parsing. Calls `parser.js`.
        - JasonViewActivity: The main Jason view.
      - **Helpe**
        - JasonHelper: Various helper methods
        - JasonSettings: Helper methods for app-wide settings
      - **Launcher**
        - You can ignore this, this just launches the app.
      - **Section**: Implements [sections](https://jasonette.github.io/documentation/document/#bodysections)
        - ItemAdapter: data binding for the scrolling section items.
        - JasonLayout: Deals with layout.
  - assets
    - parser.js: The javascript powered JSON templating engine.
    - csv.js: CSV parser
  - res
    - values
      - strings.xml: Config strings

### What files you will be touching

####User
In most cases, the only thing you will ever need to touch is the `res/values/strings.xml` file. This is where you set the main url your app will launch from, and the title of the app.

####Advanced
Sometimes you may want to write an [extension](https://jasonette.github.io/documentation/advanced/#extension). In this case you may need to deal with:
  - `Action`: To write action extension
  - `Component`: To write UI component extension

####Guru
If you find a bug **anywhere in the code**, or have any improvements anywhere else, please feel free to:
  1. Fork the `develop` branch
  2. Create a feature branch
  3. Fix
  4. Send a pull request
