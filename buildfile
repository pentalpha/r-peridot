repositories.remote << 'http://www.ibiblio.org/maven2/'
repositories.remote << 'http://repo1.maven.org/maven2'

VERSION_NUMBER = '1.0'
TARGET_JVM = '1.8'

desc 'Software for differential expression analysis on gene count reads data'
define 'r-peridot' do
  project.version = VERSION_NUMBER
  project.group = 'BioME'
  compile.with Dir[_("lib/*.jar")]
  compile.options.target = TARGET_JVM
  compile.into _('out')

  package :file=>_("jar/r-peridot.jar")
  package(:jar).with(:manifest=>_("src/main/MANIFEST.MF")).exclude('.scala-deps').merge(compile.dependencies)
end