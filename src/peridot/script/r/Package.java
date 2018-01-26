package peridot.script.r;

public class Package {
    public VersionNumber version;
    public String name;

    public Package(String name, String version) {
        this.version = new VersionNumber(version);
        this.name = name;
    }

    public boolean isOlderThan(Package aPackage) {
        return this.version.compareTo(aPackage.version) == 1;
    }

    public boolean isNewerThan(Package aPackage) {
        return this.version.compareTo(aPackage.version) == -1;
    }

    public boolean sameName(Package aPackage) {
        return name.equals(aPackage.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Package) {
            Package other = (Package)obj;
            return (sameName(other) && (!(isNewerThan(other))) && (!(isOlderThan(other))));
        }else{
            return false;
        }
    }
}