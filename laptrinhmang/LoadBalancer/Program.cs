
using Microsoft.Extensions.Configuration;
using Yarp.ReverseProxy;

var builder = WebApplication.CreateBuilder(args);

// Dump cấu hình ReverseProxy thực tế
var rpSection = builder.Configuration.GetSection("ReverseProxy");
Console.WriteLine("=== EFFECTIVE ReverseProxy CONFIG ===");
foreach (var child in rpSection.GetChildren())
{
    foreach (var sub in child.GetChildren())
    {
        Console.WriteLine($"{sub.Path} = {sub.Value}");
    }
}

builder.Services.AddReverseProxy().LoadFromConfig(rpSection);

var app = builder.Build();
app.MapReverseProxy();
app.Run();
