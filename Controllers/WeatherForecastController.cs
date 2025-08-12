using Microsoft.AspNetCore.Mvc;

namespace DotNetCore_API_Guia.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class WeatherForecastController : ControllerBase
    {
        [HttpGet]
        public IActionResult Get()
        {
            return Ok(new { mensaje = "API funcionando correctamente" });
        }
    }
}
