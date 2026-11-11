using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

namespace LibraryOS
{
    static class AppMain
    {
        [STAThread]
        static void Main(string[] args)
        {
            try
            {
                string baseDir = AppDomain.CurrentDomain.BaseDirectory;

                // 1. Try bundled jpackage executable (self-contained, no external Java needed)
                string bundledExe = Path.Combine(baseDir, "dist", "LibraryOS", "LibraryOS.exe");
                if (File.Exists(bundledExe))
                {
                    ProcessStartInfo psi = new ProcessStartInfo
                    {
                        FileName = bundledExe,
                        WorkingDirectory = baseDir,
                        Arguments = string.Join(" ", args),
                        UseShellExecute = false
                    };
                    Process.Start(psi);
                    return;
                }

                // 2. Direct JVM launcher fallback
                string javaExe = FindJava(baseDir);
                if (string.IsNullOrEmpty(javaExe))
                {
                    MessageBox.Show(
                        "Java 25 runtime was not found.\n\nPlease install Java 25 LTS or verify JAVA_HOME.",
                        "LibraryOS - Launcher",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Warning);
                    return;
                }

                string cp = BuildClasspath(baseDir);
                ProcessStartInfo javaPsi = new ProcessStartInfo
                {
                    FileName = javaExe,
                    WorkingDirectory = baseDir,
                    Arguments = "-cp \"" + cp + "\" com.libraryos.app.AppLauncher " + string.Join(" ", args),
                    UseShellExecute = false
                };
                Process.Start(javaPsi);
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    "Error launching LibraryOS:\n\n" + ex.Message,
                    "LibraryOS - Error",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
            }
        }

        static string FindJava(string baseDir)
        {
            string bundledJava = Path.Combine(baseDir, "dist", "LibraryOS", "runtime", "bin", "javaw.exe");
            if (File.Exists(bundledJava)) return bundledJava;

            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                string javaHomeBin = Path.Combine(javaHome, "bin", "javaw.exe");
                if (File.Exists(javaHomeBin)) return javaHomeBin;
            }

            string standardPath = @"C:\Program Files\Java\jdk-25.0.5\bin\javaw.exe";
            if (File.Exists(standardPath)) return standardPath;

            string latestPath = @"C:\Program Files\Java\latest\bin\javaw.exe";
            if (File.Exists(latestPath)) return latestPath;

            return "javaw.exe";
        }

        static string BuildClasspath(string baseDir)
        {
            string mainJar = Path.Combine(baseDir, "target", "libraryos-1.0.0-SNAPSHOT.jar");
            string libDir = Path.Combine(baseDir, "target", "lib", "*");
            return mainJar + ";" + libDir;
        }
    }
}
