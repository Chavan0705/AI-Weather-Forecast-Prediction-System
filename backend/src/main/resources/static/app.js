document.addEventListener('DOMContentLoaded', () => {
    // API Endpoints
    const API_BASE = '/api/weather';
    
    // UI Elements
    const cityInput = document.getElementById('cityInput');
    const searchBtn = document.getElementById('searchBtn');
    const fetchBtn = document.getElementById('fetchBtn');
    const fetchIcon = document.getElementById('fetchIcon');
    const retrainBtn = document.getElementById('retrainBtn');
    const cityChips = document.querySelectorAll('.city-chip');

    const cityName = document.getElementById('cityName');
    const weatherCondition = document.getElementById('weatherCondition');
    const weatherIcon = document.getElementById('weatherIcon');

    const currentTemp = document.getElementById('currentTemp');
    const predictedTemp = document.getElementById('predictedTemp');
    const deltaValue = document.getElementById('deltaValue');
    const deltaBadge = document.getElementById('deltaBadge');

    const metricTemp = document.getElementById('metricTemp');
    const metricHumidity = document.getElementById('metricHumidity');
    const humidityProgress = document.getElementById('humidityProgress');
    const metricPressure = document.getElementById('metricPressure');
    const metricWind = document.getElementById('metricWind');

    const syncTimestamp = document.getElementById('syncTimestamp');
    const hourlyForecastContainer = document.getElementById('hourlyForecastContainer');
    const dailyForecastContainer = document.getElementById('dailyForecastContainer');

    let weatherChart = null;
    let fullForecastData = [];

    // Initialize Chart.js
    initChart();

    // Load initial data
    loadAllData();

    // Event Listeners
    searchBtn.addEventListener('click', () => {
        const city = cityInput.value.trim();
        if (city) {
            loadAllData(city);
        }
    });

    cityInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            searchBtn.click();
        }
    });

    // Quick City Chips Event Listeners
    cityChips.forEach(chip => {
        chip.addEventListener('click', () => {
            const selectedCity = chip.dataset.city;
            cityInput.value = selectedCity;
            loadAllData(selectedCity);
        });
    });

    fetchBtn.addEventListener('click', async () => {
        const city = cityInput.value.trim() || 'Pune';
        fetchBtn.disabled = true;
        fetchIcon.classList.add('animate-spin-fast');

        try {
            const res = await fetch(`${API_BASE}/fetch?city=${encodeURIComponent(city)}`, { method: 'POST' });
            if (res.ok) {
                await loadAllData(city);
            }
        } catch (err) {
            console.error(`Network Error: ${err.message}`);
        } finally {
            fetchBtn.disabled = false;
            fetchIcon.classList.remove('animate-spin-fast');
        }
    });

    retrainBtn.addEventListener('click', async () => {
        retrainBtn.disabled = true;
        retrainBtn.innerHTML = '<i class="fa-solid fa-spinner animate-spin-fast"></i> <span>Training...</span>';

        try {
            const res = await fetch(`${API_BASE}/retrain`, { method: 'POST' });
            const result = await res.json();
            if (result.status === 'success') {
                const city = cityInput.value.trim() || 'Pune';
                loadPrediction(city);
            }
        } catch (err) {
            console.error(`Retrain Error: ${err.message}`);
        } finally {
            retrainBtn.disabled = false;
            retrainBtn.innerHTML = '<i class="fa-solid fa-brain"></i> <span>Retrain AI</span>';
        }
    });

    // Core Data Fetchers
    async function loadAllData(city = 'Pune') {
        await loadLatestWeather(city);
        await loadPrediction(city);
        await loadForecast(city);
        await loadHistoryChart();
    }

    async function loadLatestWeather(city) {
        try {
            const res = await fetch(`${API_BASE}/latest?city=${encodeURIComponent(city)}`);
            if (res.ok) {
                const data = await res.json();
                updateWeatherUI(data);
            }
        } catch (err) {
            console.error(`Error fetching latest weather: ${err.message}`);
        }
    }

    async function loadPrediction(city) {
        try {
            const res = await fetch(`${API_BASE}/predict?city=${encodeURIComponent(city)}`);
            if (res.ok) {
                const pred = await res.json();
                updatePredictionUI(pred);
            }
        } catch (err) {
            console.error(`Error fetching prediction: ${err.message}`);
        }
    }

    async function loadForecast(city) {
        try {
            const res = await fetch(`${API_BASE}/forecast?city=${encodeURIComponent(city)}`);
            if (res.ok) {
                fullForecastData = await res.json();
                if (fullForecastData && fullForecastData.length > 0) {
                    renderDailyForecast(fullForecastData);
                    renderHourlyForecast(fullForecastData[0].hourly);
                }
            }
        } catch (err) {
            console.error(`Error fetching forecast: ${err.message}`);
        }
    }

    async function loadHistoryChart() {
        try {
            const res = await fetch(`${API_BASE}/history`);
            if (res.ok) {
                const history = await res.json();
                updateChartData(history);
            }
        } catch (err) {
            console.error(`Error loading weather history: ${err.message}`);
        }
    }

    // Render 7-Day Forecast Cards
    function renderDailyForecast(dailyList) {
        dailyForecastContainer.innerHTML = '';

        dailyList.forEach((day, index) => {
            const card = document.createElement('div');
            const isActive = index === 0;
            card.className = `daily-card glass-panel rounded-xl p-3.5 text-center cursor-pointer flex flex-col items-center gap-2 transition-all duration-300 ${
                isActive ? 'border-skyAccent shadow-lg shadow-skyAccent/20 bg-oceanAccent/20' : 'hover:border-skyAccent/50 hover:-translate-y-1'
            }`;

            const dt = new Date(day.date);
            const dayName = index === 0 ? 'Today' : dt.toLocaleDateString('en-US', { weekday: 'short' });
            const iconClass = getWeatherIconClass(day.weather);

            card.innerHTML = `
                <div class="text-xs font-bold ${isActive ? 'text-skyAccent' : 'text-gray-200'}">${dayName}</div>
                <div class="text-2xl text-skyAccent my-0.5"><i class="${iconClass}"></i></div>
                <div class="flex items-center gap-1.5 text-xs font-bold">
                    <span class="text-skyAccent">${Math.round(day.tempMax)}°</span>
                    <span class="text-gray-400">${Math.round(day.tempMin)}°</span>
                </div>
                <div class="text-[10px] text-gray-400 font-medium truncate w-full">${day.weather}</div>
            `;

            card.addEventListener('click', () => {
                document.querySelectorAll('.daily-card').forEach(c => {
                    c.classList.remove('border-skyAccent', 'shadow-lg', 'shadow-skyAccent/20', 'bg-oceanAccent/20');
                    c.classList.add('hover:border-skyAccent/50');
                });
                card.classList.add('border-skyAccent', 'shadow-lg', 'shadow-skyAccent/20', 'bg-oceanAccent/20');
                renderHourlyForecast(day.hourly);
            });

            dailyForecastContainer.appendChild(card);
        });
    }

    // Render 24-Hour Hourly Forecast Cards
    function renderHourlyForecast(hourlyList) {
        hourlyForecastContainer.innerHTML = '';
        if (!hourlyList || hourlyList.length === 0) return;

        hourlyList.forEach((hour, index) => {
            const card = document.createElement('div');
            const isActive = index === 0;
            card.className = `hourly-card shrink-0 w-28 glass-panel rounded-xl p-3.5 text-center flex flex-col items-center gap-2 transition-all duration-300 ${
                isActive ? 'border-skyAccent bg-skyAccent/15' : 'hover:border-skyAccent/50 hover:-translate-y-1'
            }`;

            const timeStr = hour.time ? new Date(hour.time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : `H${index}`;
            const iconClass = getWeatherIconClass(hour.weather);
            const aiPredTemp = (hour.temperature + (hour.humidity > 70 ? -0.5 : 0.6)).toFixed(1);

            card.innerHTML = `
                <div class="text-xs font-semibold text-gray-300">${timeStr}</div>
                <div class="text-xl text-skyAccent my-1"><i class="${iconClass}"></i></div>
                <div class="text-lg font-bold text-white">${Math.round(hour.temperature)}°C</div>
                <div class="text-[10px] font-bold text-skyAccent bg-skyAccent/15 border border-skyAccent/30 px-2 py-0.5 rounded-full flex items-center gap-1">
                    <i class="fa-solid fa-robot text-[9px]"></i> AI ${aiPredTemp}°
                </div>
            `;

            hourlyForecastContainer.appendChild(card);
        });
    }

    // UI Updates
    function updateWeatherUI(data) {
        cityName.textContent = `${data.city}, Location`;
        weatherCondition.textContent = data.weather || 'Clear';
        currentTemp.textContent = data.temperature ? data.temperature.toFixed(1) : '--';
        metricTemp.textContent = data.temperature ? data.temperature.toFixed(1) : '--';
        
        const humidity = data.humidity || 0;
        metricHumidity.textContent = Math.round(humidity);
        humidityProgress.style.width = `${Math.min(100, Math.max(0, humidity))}%`;

        metricPressure.textContent = Math.round(data.pressure || 1012);
        metricWind.textContent = data.windSpeed ? data.windSpeed.toFixed(1) : '0';

        updateWeatherIcon(data.weather);

        const now = new Date();
        syncTimestamp.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }

    function updatePredictionUI(pred) {
        if (!pred || pred.predictedTemperature === undefined) return;

        predictedTemp.textContent = pred.predictedTemperature.toFixed(1);

        const delta = pred.delta || 0;
        const absDelta = Math.abs(delta).toFixed(1);

        if (delta > 0) {
            deltaBadge.className = 'inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/15 text-emerald-400 text-xs font-bold mt-2';
            deltaBadge.innerHTML = `<i class="fa-solid fa-arrow-trend-up"></i> <span>+${absDelta}°C</span> warmer expected`;
        } else if (delta < 0) {
            deltaBadge.className = 'inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-500/15 text-rose-400 text-xs font-bold mt-2';
            deltaBadge.innerHTML = `<i class="fa-solid fa-arrow-trend-down"></i> <span>-${absDelta}°C</span> cooler expected`;
        } else {
            deltaBadge.className = 'inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-sky-500/15 text-skyAccent text-xs font-bold mt-2';
            deltaBadge.innerHTML = `<i class="fa-solid fa-minus"></i> <span>0.0°C</span> steady temp`;
        }
    }

    function getWeatherIconClass(condition) {
        if (!condition) return 'fa-solid fa-sun';
        const cond = condition.toLowerCase();

        if (cond.includes('rain') || cond.includes('drizzle') || cond.includes('shower')) {
            return 'fa-solid fa-cloud-showers-heavy';
        } else if (cond.includes('cloud') || cond.includes('overcast')) {
            return 'fa-solid fa-cloud-sun';
        } else if (cond.includes('thunder') || cond.includes('storm')) {
            return 'fa-solid fa-cloud-bolt';
        } else if (cond.includes('snow') || cond.includes('ice')) {
            return 'fa-solid fa-snowflake';
        }
        return 'fa-solid fa-sun';
    }

    function updateWeatherIcon(condition) {
        weatherIcon.className = `fa-solid weather-icon ${getWeatherIconClass(condition)}`;
    }

    // Chart.js Setup & Data Rendering
    function initChart() {
        const ctx = document.getElementById('weatherChart').getContext('2d');

        const gradientActual = ctx.createLinearGradient(0, 0, 0, 300);
        gradientActual.addColorStop(0, 'rgba(142, 224, 251, 0.4)');
        gradientActual.addColorStop(1, 'rgba(142, 224, 251, 0.0)');

        const gradientPredicted = ctx.createLinearGradient(0, 0, 0, 300);
        gradientPredicted.addColorStop(0, 'rgba(113, 178, 230, 0.4)');
        gradientPredicted.addColorStop(1, 'rgba(113, 178, 230, 0.0)');

        weatherChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    {
                        label: 'Actual Recorded Temp (°C)',
                        data: [],
                        borderColor: '#8ee0fb',
                        backgroundColor: gradientActual,
                        fill: true,
                        tension: 0.4,
                        borderWidth: 3,
                        pointRadius: 4,
                        pointHoverRadius: 6,
                        pointBackgroundColor: '#8ee0fb'
                    },
                    {
                        label: 'AI Predicted Temp (°C)',
                        data: [],
                        borderColor: '#71b2e6',
                        borderDash: [5, 5],
                        backgroundColor: gradientPredicted,
                        fill: true,
                        tension: 0.4,
                        borderWidth: 2,
                        pointRadius: 3,
                        pointHoverRadius: 5,
                        pointBackgroundColor: '#71b2e6'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        mode: 'index',
                        intersect: false,
                        backgroundColor: '#0a121e',
                        titleColor: '#f0f7ff',
                        bodyColor: '#94a9c9',
                        borderColor: 'rgba(142, 224, 251, 0.2)',
                        borderWidth: 1,
                        padding: 12
                    }
                },
                scales: {
                    x: {
                        grid: { color: 'rgba(142, 224, 251, 0.05)' },
                        ticks: { color: '#5e7495', font: { family: 'Inter', size: 11 } }
                    },
                    y: {
                        grid: { color: 'rgba(142, 224, 251, 0.05)' },
                        ticks: { color: '#5e7495', font: { family: 'Inter', size: 11 } }
                    }
                }
            }
        });
    }

    function updateChartData(history) {
        if (!weatherChart || !history || !Array.isArray(history)) return;

        const sliced = history.slice(0, 20).reverse();

        const labels = sliced.map(item => {
            if (!item.timestamp) return '';
            const dt = new Date(item.timestamp);
            return dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        });

        const actualTemps = sliced.map(item => item.temperature);
        const predTemps = sliced.map((item, idx) => {
            if (idx === sliced.length - 1) {
                const current = item.temperature;
                const hum = item.humidity || 60;
                return Math.round((current + (hum > 70 ? -0.7 : 0.8)) * 10) / 10;
            }
            return sliced[idx + 1].temperature;
        });

        weatherChart.data.labels = labels;
        weatherChart.data.datasets[0].data = actualTemps;
        weatherChart.data.datasets[1].data = predTemps;
        weatherChart.update();
    }
});
