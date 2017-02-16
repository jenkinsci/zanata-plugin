#!/bin/bash

# usage: $BASH_SOURCE[0] pluginA[:version] pluginB 
for plugin in $@; do
    # set the internal field separator (IFS) variable, and then let it parse into an array.
    # When this happens in a command, then the assignment to IFS only takes place to that single command's environment (to read ).
    # It then parses the input according to the IFS variable value into an array
    IFS=':' read -ra pluginVer <<< "$plugin"
    if [ ${#pluginVer[@]} -eq 2 ]
    then
        install-plugins.sh $pluginVer[0] $pluginVer[1]
    else
	install-plugins.sh $plugin
    fi
done

