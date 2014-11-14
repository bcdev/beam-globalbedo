import os
from string import Template

class Request:
  _templateFileName = ''

  def __init__(self, templateFileName):
    self._templateFileName = templateFileName

  def generate(self, keywords, requestFileName):
    with open(self._templateFileName, 'r') as templateFile:
      template_data = templateFile.read()
    templateFile.closed

    content = Template(template_data).safe_substitute(keywords)

    with open(requestFileName, 'w') as requestFile:
      requestFile.write(content)
    requestFile.closed

