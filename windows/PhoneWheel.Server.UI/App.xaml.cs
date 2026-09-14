using System.Windows;
using System.Threading.Tasks;
using System;
using System.IO;

namespace PhoneWheel.Server.UI;

/// <summary>
/// Ponto de entrada do aplicativo WPF.
/// </summary>
public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        // UI thread exceptions
        this.DispatcherUnhandledException += (s, args) =>
        {
            LogException("DispatcherUnhandledException", args.Exception);
            args.Handled = true; // Prevent crash
        };

        // Task exceptions
        TaskScheduler.UnobservedTaskException += (s, args) =>
        {
            LogException("UnobservedTaskException", args.Exception);
            args.SetObserved(); // Prevent crash
        };

        // Non-UI thread exceptions
        AppDomain.CurrentDomain.UnhandledException += (s, args) =>
        {
            LogException("AppDomain.UnhandledException", args.ExceptionObject as Exception);
            // This one might still crash depending on the exception, but we try to log it.
        };
    }

    private void LogException(string source, Exception? ex)
    {
        try
        {
            var logPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "crash.log");
            var message = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] {source}\n{ex?.ToString()}\n\n";
            File.AppendAllText(logPath, message);
        }
        catch 
        {
            // Ignore errors while logging
        }
    }
}