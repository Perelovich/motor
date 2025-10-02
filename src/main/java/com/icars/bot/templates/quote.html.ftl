<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Коммерческое предложение №${order.publicId}</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 2rem; color: #333; }
        .container { max-width: 800px; margin: auto; border: 1px solid #eee; padding: 2rem; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.05); }
        .header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #007bff; padding-bottom: 1rem; margin-bottom: 2rem; }
        .header h1 { margin: 0; color: #007bff; }
        .header-info { text-align: right; }
        .logo { font-weight: bold; font-size: 1.5rem; color: #333; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 2rem; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #f8f9fa; }
        .total-section { text-align: right; margin-top: 2rem; }
        .total-section p { font-size: 1.2rem; font-weight: bold; }
        .footer { margin-top: 3rem; padding-top: 1rem; border-top: 1px solid #eee; font-size: 0.9rem; color: #777; }
        /* TODO: Implement PDF generation styles, e.g., using Flying Saucer PDF Renderer */
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <div>
            <div class="logo">ICars Powertrain</div>
            <h1>Коммерческое предложение</h1>
        </div>
        <div class="header-info">
            <p><strong>Номер:</strong> ${order.publicId}</p>
            <p><strong>Дата:</strong> ${quoteDate?string("dd.MM.yyyy")}</p>
        </div>
    </div>

    <h3>Заказчик: ${order.customerName}</h3>

    <table>
        <thead>
        <tr>
            <th colspan="2">Параметры запрашиваемого агрегата</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td>Автомобиль</td>
            <td>${engine.make} ${engine.model}, ${engine.year} г.в.</td>
        </tr>
        <tr>
            <td>VIN</td>
            <td>${engine.vin}</td>
        </tr>
        <tr>
            <td>Двигатель</td>
            <td>${engine.engineCodeOrDetails}</td>
        </tr>
        <tr>
            <td>Комплектность</td>
            <td>${engine.kitDetails}</td>
        </tr>
        </tbody>
    </table>

    <table>
        <thead>
        <tr>
            <th>Наименование</th>
            <th>Стоимость</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td>Двигатель ${engine.make} ${engine.model} (${engine.engineCodeOrDetails})</td>
            <td>${price} руб.</td>
        </tr>
        <tr>
            <td>Доставка до г. ${order.deliveryCity}</td>
            <td>${deliveryCost} руб.</td>
        </tr>
        </tbody>
    </table>

    <div class="total-section">
        <p>Итого к оплате: ${totalPrice} руб.</p>
    </div>

    <div class="footer">
        <h4>Условия поставки:</h4>
        <ul>
            <li>Ориентировочный срок доставки: ${deliveryTime} дней.</li>
            <li>Предоплата: 50%.</li>
            <li>Гарантия: 30 дней с момента установки.</li>
            <li>Предложение действительно в течение 7 дней.</li>
        </ul>
        <p>С уважением, команда ICars.</p>
    </div>
</div>
</body>
</html>
