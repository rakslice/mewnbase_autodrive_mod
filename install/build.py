import os
import os.path
import urllib.request
import subprocess
from zipfile import ZipFile
import sys
import shutil
from email.utils import formatdate
import argparse


if sys.platform == "win32":
    program_files = os.environ.get("ProgramW6432")
    if program_files is None:
        program_files = os.environ["ProgramFiles"]
    DIFF_PROGRAM = os.path.join(program_files, "git", "usr", "bin", "diff.exe")
else:
    DIFF_PROGRAM = "diff"


jd_wrapper_jd_cli_zip_url = """https://github.com/jd-wrapper/jd-cli/archive/refs/tags/v1.0.0.zip"""

script_path = os.path.dirname(os.path.abspath(__file__))


def desuffix(s, suffix):
    assert s.endswith(suffix)
    return s[:-len(suffix)]


def web_download_file(url, output_filename):
    urllib.request.urlretrieve(url, output_filename)


def contents(filename):
    with open(filename, "rb") as handle:
        return handle.read()
    
    
def write_contents(filename, data):
    with open(filename, "wb") as handle:
        handle.write(data)
        
        
def diff_u(first_file, second_file):
    p = subprocess.Popen([DIFF_PROGRAM, "-uw", first_file, second_file])
    # -u produces a diff in unified format
    # -w ignores all whitespace differences
    
    p.wait()
    return p.returncode
        

def file_pattern_replace(filename, old, new, new_filename=None):
    if new_filename is None:
        new_filename = filename
    data = contents(filename)
    if old in data:
        write_contents(new_filename, data.replace(old, new))


def gradle_build_dir(code_dir):
    if sys.platform == "win32":
        command_file = "gradlew.bat"
    else:
        command_file = "gradlew"
        
    subprocess.check_call([os.path.join(code_dir, command_file), "assemble"], cwd=code_dir)


def get_downloaded_file(url, dir):
    filename_proper = url.rsplit("/", 1)[-1]
    filename = os.path.join(dir, filename_proper)    
    if not os.path.exists(filename):
        print("Downloading %s to %s" % (url, dir))
        web_download_file(url, filename)
    else:
        print("Using already downloaded %d" % filename_proper)
    return filename


def unzip_file_to_dir(zip_file, dest_dir):
    with ZipFile(zip_file, "r") as zipObj:
        zipObj.extractall(path=dest_dir)


class JavaDecompiler(object):
    def __init__(self):
        self.cli_jar = None
        
    def setup(self):
        jd_cli_dir = os.path.join(script_path, "jd-cli")
        if not os.path.isdir(jd_cli_dir):
            jd_cli_zip_filename = get_downloaded_file(jd_wrapper_jd_cli_zip_url, script_path)
            
            unzip_file_to_dir(jd_cli_zip_filename, jd_cli_dir)
        
        code_dir = os.path.join(jd_cli_dir, "jd-cli-1.0.0")

        gradle_build_dir(code_dir)

        libs_dir = os.path.join(code_dir, "build", "libs")
        libs_files = os.listdir(libs_dir)
        
        libs_files = [x for x in libs_files if "-min" not in x]
        
        assert len(libs_files) == 1
        
        jd_cli_jar = os.path.join(libs_dir, libs_files[0])
        assert jd_cli_jar.endswith(".jar")
        
        print("using jd core %s" % jd_cli_jar)
        self.cli_jar = jd_cli_jar
        
    def ensure_file_dir_exists(self, dest_file):
        dest_inner_dir = os.path.dirname(dest_file)
        os.makedirs(dest_inner_dir, exist_ok=True)
        
    def decompile_jar_to_dir(self, jar_filename, dest_dir, class_pathspec):
        if self.cli_jar is None:
            raise FileNotFoundError("jd core location unknown; run setup() first")
       
        assert os.path.isfile(jar_filename)
        
        dest_file_rel = desuffix(class_pathspec, ".class") + ".java"
        dest_file = os.path.join(dest_dir, dest_file_rel)
        self.ensure_file_dir_exists(dest_file)

        # see docs at https://github.com/jd-wrapper/jd-cli
        cli_command_line_options = ["-b", jar_filename, class_pathspec]

        output = subprocess.check_output(["java", "-jar", self.cli_jar] + cli_command_line_options)
        
        write_contents(dest_file, output)
        
        return dest_file
    
    
class ModifiedJavaProject(object):
    
    def __init__(self, decompiler, original_jar, decomp_dir, extracted_classpath_dir, additional_src_dir):
        """
        This represents a Java project originally distributed as a JAR that is being used with modified code
        
        @param original_jar: The unmodified original JAR
        @param decomp_dir: A working directory we will decompile individual original classes to
        @param extracted_classpath_dir: A classpath directory for runs with the modifications, that contains
                                        all original class files from the original JAR that we are using as-is in
                                        the modified version    
        @param additional_src_dir: The source directory with the additional source added in the modification,
                                   that we will also add generated source files to
        """
        
        assert isinstance(decompiler, JavaDecompiler)
        self.decompiler = decompiler
        self.original_jar_filename = original_jar
        self.decomp_dir = decomp_dir
        self.extracted_classpath_dir = extracted_classpath_dir
        self.src_dir = additional_src_dir
        
    def additional_src_filename(self, class_pathspec):
        return os.path.join(self.src_dir, desuffix(class_pathspec, ".class") + ".java")
    
    def class_name_proper(self, class_pathspec):
        return desuffix(os.path.basename(class_pathspec), ".class")
        
    def common_decompile_and_remove_unpatched(self, class_pathspec):
        """
        Decompile the original class from the JAR
        @param class_pathspec: fully qualified class file specifier using forward slash as the separator and ending with ".class"
        @return: the filename of the extracted source file
        """
        
        class_name = self.class_name_proper(class_pathspec)

        decompiled_code_filename = self.decompiler.decompile_jar_to_dir(self.original_jar_filename, self.decomp_dir, class_pathspec)
        
        our_version = self.additional_src_filename(class_pathspec)
        
        # If the class file is still in the extracted version, remove it, since we want to use the one built from the modified source code instead
        extracted_class_file = os.path.join(self.extracted_classpath_dir, class_pathspec)
        if os.path.isfile(extracted_class_file):
            print("deleting " + extracted_class_file)
            os.remove(extracted_class_file)
            
        return decompiled_code_filename
    
    def make_constructor_accessible(self, class_pathspec, show_differences=True):
        """
        Decompile the source for the given class and make the default (class Foo()) constructor accessible
        @param class_pathspec: fully qualified class file specifier using forward slash as the separator and ending with ".class"
        """
        
        decompiled_code_filename = self.common_decompile_and_remove_unpatched(class_pathspec)
        
        # Create a version of the extracted file with added constructor
        
        new_filename = self.additional_src_filename(class_pathspec)
        
        class_name = self.class_name_proper(class_pathspec)

        orig_class_start_fragment = "public class %s {" % class_name
        
        orig_class_start_fragment_bytes = orig_class_start_fragment.encode("utf-8")

        orig_contents = contents(decompiled_code_filename)
        assert orig_class_start_fragment_bytes in orig_contents
        
        private_constructor_start = "private %s() {" % class_name
        has_private_constructor = private_constructor_start.encode("utf-8") in orig_contents
        protected_constructor_start = "protected %s() {" % class_name
        has_protected_constructor = protected_constructor_start.encode("utf-8") in orig_contents
        has_public_constructor = ("public %s() {" % class_name).encode("utf-8") in orig_contents
        
        if has_protected_constructor or has_public_constructor:
            print("class %s already has an accessible default constructor")
            shutil.copy(decompiled_code_filename, new_filename)
            return
        
        note = "// protected constructor version generated by %s %s\n" % (sys.argv[0], formatdate())
        print("replacing " + new_filename)

        if has_private_constructor:
            # we need to change the visibility of the existing constructor
            orig_class_start_fragment_bytes = private_constructor_start.encode("utf-8")
            replacement_fragment = note + protected_constructor_start
            replacement_fragment_bytes = replacement_fragment.encode("utf-8")
            self.decompiler.ensure_file_dir_exists(new_filename)
            file_pattern_replace(decompiled_code_filename, orig_class_start_fragment_bytes, replacement_fragment_bytes, new_filename)
        else:
            # we need to add a constructor
            protected_constructor = "protected %s() { }" % class_name
            
            replacement_fragment = orig_class_start_fragment + "\n" + note + protected_constructor
            replacement_fragment_bytes = replacement_fragment.encode("utf-8")
            
            self.decompiler.ensure_file_dir_exists(new_filename)
            file_pattern_replace(decompiled_code_filename, orig_class_start_fragment_bytes, replacement_fragment_bytes, new_filename)
            
        if show_differences:
            diff_u(decompiled_code_filename, new_filename)
            
    def get_class_fully_qualified_name(self, class_pathspec):
        return desuffix(class_pathspec, ".class").replace("/", ".")
            
    def replace_constructed_object(self, class_pathspec, original_constructed_pathspec, new_constructed_pathspec):
        """ Decompile the given class and replace calls to construct one class of object
        with calls to construct another class of object
        @param class_pathspec: fully qualified class file specifier using forward slash as the separator and ending with ".class"
        @param original_constructed_pathspec: The class of constructor calls to replace
        @param new_constructed_pathspec: The class to replace it with
        """
        
        decompiled_code_filename = self.common_decompile_and_remove_unpatched(class_pathspec)
        
        source_code = contents(decompiled_code_filename).decode("utf-8")
        
        note = "// alternate constructor call version generated by %s %s\n" % (sys.argv[0], formatdate())
        
        # note goes at top
        source_code = note + source_code

        full_original_constructed_class_name = self.get_class_fully_qualified_name(original_constructed_pathspec)
        
        constructor_match_classes = [full_original_constructed_class_name]
        
        has_original_class_import = ("import %s;" % full_original_constructed_class_name) in source_code
        
        if has_original_class_import:
            constructor_match_classes.append(self.class_name_proper(original_constructed_pathspec))
            
        full_replacement_constructed_class_name = self.get_class_fully_qualified_name(new_constructed_pathspec)
        
        some_match = False
        for cons_match in constructor_match_classes:
            constructor_call_form = "new %s("
            constructor_call_match = constructor_call_form % cons_match
            print(constructor_call_match)
            if constructor_call_match in source_code:
                some_match = True
            source_code = source_code.replace(constructor_call_match, constructor_call_form % full_replacement_constructed_class_name)
            
        assert some_match, "no matching constructor calls found"
        
        self.common_finish_contents(class_pathspec, source_code, decompiled_code_filename)
        
    def make_methods_accessible(self, class_pathspec, method_specs_list):
        """
        Decompile the source for the given class and make the given methods visible
        @param method_specs_list: a list of specifiers for methods to make visible, 
                                    where a specifier is a string with a return type like "void foo"
        @param class_pathspec: fully qualified class file specifier using forward slash as the separator and ending with ".class"
        """
        
        decompiled_code_filename = self.common_decompile_and_remove_unpatched(class_pathspec)
        
        source_code = contents(decompiled_code_filename).decode("utf-8")
        
        note = "// accessible methods version generated by %s %s\n" % (sys.argv[0], formatdate())
        
        # note goes at top
        source_code = note + source_code
        
        for method_spec in method_specs_list:
            source_code = source_code.replace("private %s(" % method_spec, "protected %s(" % method_spec)
        
        self.common_finish_contents(class_pathspec, source_code, decompiled_code_filename)
        
    def common_finish_contents(self, class_pathspec, source_code_str, decompiled_code_filename):
        new_filename = self.additional_src_filename(class_pathspec)

        print("replacing " + new_filename)
        self.decompiler.ensure_file_dir_exists(new_filename)
        write_contents(new_filename, source_code_str.encode("utf-8"))
        
        diff_u(decompiled_code_filename, new_filename)
        

def get_java_bin_path(program):
    assert program == "javac" or program == "jar"
    if program == "javac":
        desc = "A Java compiler"
    elif program == "jar":
        desc = "The jar tool"
    
    if sys.platform == "win32":
        general_instructions = "\nPlease make sure you have a JDK installed and the JAVA_HOME environment variable is set to its path in Advanced System Settings."
        if "JAVA_HOME" not in os.environ:
            die("The JAVA_HOME environment variable is not set." + general_instructions)
        java_home = os.environ["JAVA_HOME"]
        if not os.path.isdir(java_home):
            die(("The JAVA_HOME environment variable is set to '%s', which does not appear to be a directory." % java_home) + general_instructions)
        result = os.path.join(java_home, "bin", "%s.exe" % program)
        if not os.path.isfile(result):
            die((desc + " was not found in the JAVA_HOME '%s'" % java_home) + general_instructions)
    else:
        result = program
    return result

        
def compile_java_dir(src_dir, bin_dir, classpath_dirs=None):
    """
    @param src_dir: Java source directory for the top level package
    @param bin_dir: Destination directory for compiler output class files, top level of package hierarchy
    @param classpath_dirs: List of directories or JARs to use as the classpath in addition to normal system library path when building
    """
    javac = get_java_bin_path("javac")
    
    source_files = []
    for dirpath, dirnames, filenames in os.walk(src_dir):
        for filename in filenames:
            if filename.endswith(".java"):
                full_filename = os.path.join(dirpath, filename)
                source_files.append(full_filename)    
    
    print("passing javac %d source files" % len(source_files))
    args = [javac, "-d", bin_dir]
    if classpath_dirs is not None:
        classpath = ";".join(classpath_dirs)
        args += ["-cp", classpath]
    args += source_files
    subprocess.check_call(args)


def create_mod_jar(output_jar_filename, classpath_dirs, main_class):
    jar = get_java_bin_path("jar")
    
    print("creating file with initial files from %s" % classpath_dirs[0])
    subprocess.check_call([jar, "cfe", output_jar_filename, main_class, "-C", classpath_dirs[0], "."])
    for classpath_dir in classpath_dirs[1:]:
        print("adding %s" % classpath_dir)
        subprocess.check_call([jar, "uf", output_jar_filename, "-C", classpath_dir, "."])


def parse_args():
    parser = argparse.ArgumentParser("get_sources")
    parser.add_argument("mewnbase_directory", help="The directory containing the original mewnbase game")
    
    return parser.parse_args()


def die(msg):
    print(msg, file=sys.stderr)
    sys.exit(1)
    
    
def step(name):
    print("* " + name)


def main():
    options = parse_args()
    
    decomp_dir = os.path.join(script_path, "decomp")
    if not os.path.isdir(decomp_dir):
        os.mkdir(decomp_dir)
        
    extracted_classpath_dir = os.path.join(script_path, "extracted_jar")
    if not os.path.isdir(extracted_classpath_dir):
        os.mkdir(extracted_classpath_dir)
    
    # FIXME dynamically determine the existing game location to use
    mewnbase_dir = options.mewnbase_directory
    mewnbase_jar = os.path.join(mewnbase_dir, "game", "desktop-1.0.jar")
    
    if not os.path.isfile(mewnbase_jar):
        die("Main game JAR file %s not found; double check mewnbase_dir is really %s" % (mewnbase_jar, mewnbase_dir))
    
    step("Extracting game code class files from JAR")
    unzip_file_to_dir(mewnbase_jar, extracted_classpath_dir)

    jd = JavaDecompiler()
    jd.setup()
    
    step("Decompiling and modifying game files...")
    print(mewnbase_jar)
    
    additional_src_dir = os.path.abspath(os.path.join(script_path, "..", "src")) 

    modifiedGame = ModifiedJavaProject(jd, mewnbase_jar, decomp_dir, extracted_classpath_dir, additional_src_dir)
    
    # Make the necessary modifications to classes:
   
    # constructor visibility
    for class_pathspec in ["com/cairn4/moonbase/tiles/TileFactory.class",
                           "com/cairn4/moonbase/ItemFactory.class"]:
        
        modifiedGame.make_constructor_accessible(class_pathspec)
    
    # replacement classes
    modifiedGame.replace_constructed_object("com/cairn4/moonbase/ui/GameScreen.class", "com/cairn4/moonbase/GameLoader.class", "net/rakslice/mewnbase_mod/AltGameLoader.class")
    
    # method visibility 
    modifiedGame.make_methods_accessible("com/cairn4/moonbase/GameLoader.class", ["void loadEntities"])
    
    # # For getting a diff for our own hand-modified file we would do something like
    # class_pathspec = "com/cairn4/moonbase/entities/Buggie.class"
    #
    # decompiled_code_filename = jd.decompile_jar_to_dir(mewnbase_jar, decomp_dir, class_pathspec)
    # our_version = os.path.join(script_path, "..", "src", desuffix(class_pathspec, "Buggie.class") + "Train.java")
    #
    # diff_u(decompiled_code_filename, our_version)
    
    step("Compiling added mod source")
    bin_dir = os.path.abspath(os.path.join(script_path, "..", "bin"))
    compile_java_dir(additional_src_dir, bin_dir, classpath_dirs=[extracted_classpath_dir])
    
    step("Assemble modded game jar")
    output_jar_filename = os.path.join(script_path, "build", "desktop-1.0.jar")
    jd.ensure_file_dir_exists(output_jar_filename)
    if os.path.exists(output_jar_filename):
        os.remove(output_jar_filename)
    main_class = "com.cairn4.moonbase.desktop.ModDesktopLauncher"
    create_mod_jar(output_jar_filename, [bin_dir, extracted_classpath_dir], main_class)
    
    step("Done. Output at: %s" % output_jar_filename)

    
if __name__ == "__main__":
    main()
