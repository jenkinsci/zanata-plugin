FROM jenkins:2.32.2


# COPY plugins.txt /usr/share/jenkins/plugins.txt

# plugins.sh takes an explicit list of plugins and their dependencies from a file. It can not resolve dependencies
# install-plugins.sh will resolve dependencies but only install the version listed in the plugin
# RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt


ENV INIT_PLUGINS="git maven-plugin mailer credentials credentials-binding parameterized-trigger" DEBUG_PORT=8989

# copy our own utility scripts. To avoid overriding jenkins provide scripts, we prefix our scripts with z_
COPY scripts/ /usr/local/bin/
COPY zanata.hpi ${JENKINS_HOME}/plugins/

USER root
RUN chmod go+x /usr/local/bin/z_*

USER ${user}

#ENV JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=n"

RUN /usr/local/bin/z_install_plugins.sh ${INIT_PLUGINS}




