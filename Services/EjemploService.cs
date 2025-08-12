using DotNetCore_API_Guia.Models;
using System.Collections.Generic;
using System.Linq;

namespace DotNetCore_API_Guia.Services
{
    public class EjemploService
    {
        private List<EjemploModel> _datos = new List<EjemploModel>();

        public IEnumerable<EjemploModel> ObtenerTodos() => _datos;

        public void Agregar(EjemploModel modelo) => _datos.Add(modelo);
    }
}
