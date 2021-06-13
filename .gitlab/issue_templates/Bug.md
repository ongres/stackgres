<!--
Please read this!

Before opening a new issue, make sure to search for keywords in the issues
filtered by the "Bug" label.

Issue tracker:

- https://gitlab.com/ongresinc/stackgres/-/issues?label_name=Bug

and verify the issue you're about to submit isn't a duplicate.
-->
### Summary

<!--(Summarize the bug encountered concisely)-->

### Current Behaviour 


#### Steps to reproduce

<!--(How one can reproduce the issue - this is very important) -->

### Expected Behaviour


### Possible Solution

<!-- What do you think is the possible solution for the bug -->

### Environment

- StackGres version:
<!-- if you have used helm, you can use:  `helm get notes -n stackgres stackgres-operator` 
more generic: `kubectl get deployments -n stackgres stackgres-operator --template '{{ printf "%s\n" (index .spec.template.spec.containers 0).image }}'`
-->
- Kubernetes version:
<!--(use `kubectl version`) -->
- Cloud provider or hardware configuration:


### Relevant logs and/or screenshots

<!-- (Paste any relevant logs - please use code blocks (```) to format console output,
logs, and code as it's very hard to read otherwise.) -->

/label ~Bug ~StackGres ~Triage
