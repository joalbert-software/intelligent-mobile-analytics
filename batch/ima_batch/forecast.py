from __future__ import absolute_import, division

import math

from .statistics import finite
from .validation import integer


def linear_fit(points):
    values = [(finite(x), finite(y)) for x, y in points]
    if len(values) < 2:
        raise ValueError("linear fit requires at least two observations")
    mean_x = sum(x for x, unused in values) / len(values)
    mean_y = sum(y for unused, y in values) / len(values)
    denominator = sum((x - mean_x) ** 2 for x, unused in values)
    if denominator == 0:
        raise ValueError("linear fit requires different x coordinates")
    slope = sum((x - mean_x) * (y - mean_y) for x, y in values) / denominator
    intercept = mean_y - slope * mean_x
    residuals = [y - (intercept + slope * x) for x, y in values]
    squared_error = sum(value * value for value in residuals)
    total_variation = sum((y - mean_y) ** 2 for unused, y in values)
    return {"slope": slope, "intercept": intercept, "count": len(values),
            "squaredError": squared_error,
            "rSquared": 1 - squared_error / total_variation if total_variation else None,
            "residualStandardError": math.sqrt(squared_error / (len(values) - 2)) if len(values) > 2 else None}


def linear_forecast(values, horizon, nonnegative=True):
    integer(horizon, "horizon", 1, 365)
    observations = [finite(value) for value in values]
    fit = linear_fit(list(enumerate(observations)))
    predictions = []
    for offset in range(horizon):
        index = len(observations) + offset
        estimate = fit["intercept"] + fit["slope"] * index
        predictions.append(max(0.0, estimate) if nonnegative else estimate)
    return {"model": "ordinary_least_squares_trend", "fit": fit, "predictions": predictions}


def seasonal_naive(values, period, horizon):
    integer(period, "period", 1, 365)
    integer(horizon, "horizon", 1, 365)
    values = [finite(value) for value in values]
    if len(values) < period:
        raise ValueError("seasonal baseline requires a complete period")
    last_period = values[-period:]
    return [last_period[index % period] for index in range(horizon)]


def holt(values, horizon, alpha=0.3, beta=0.1, nonnegative=True):
    values = [finite(value) for value in values]
    integer(horizon, "horizon", 1, 365)
    alpha = finite(alpha)
    beta = finite(beta)
    if not 0 < alpha <= 1 or not 0 <= beta <= 1:
        raise ValueError("invalid smoothing coefficients")
    if len(values) < 2:
        raise ValueError("Holt trend requires two observations")
    level = values[0]
    trend = values[1] - values[0]
    fitted = []
    for value in values[1:]:
        fitted.append(level + trend)
        previous = level
        level = alpha * value + (1 - alpha) * (level + trend)
        trend = beta * (level - previous) + (1 - beta) * trend
    predictions = [level + (offset + 1) * trend for offset in range(horizon)]
    if nonnegative:
        predictions = [max(0.0, value) for value in predictions]
    return {"model": "holt_linear", "level": level, "trend": trend,
            "alpha": alpha, "beta": beta, "fitted": fitted, "predictions": predictions}


def error_metrics(actual, predicted):
    actual = [finite(value) for value in actual]
    predicted = [finite(value) for value in predicted]
    if len(actual) != len(predicted) or not actual:
        raise ValueError("actual and predicted vectors must have the same nonzero length")
    errors = [prediction - observation for observation, prediction in zip(actual, predicted)]
    absolute = [abs(value) for value in errors]
    percentage = [abs(error / observation) for observation, error in zip(actual, errors) if observation != 0]
    denominator = sum(abs(value) for value in actual)
    return {
        "count": len(actual),
        "meanError": sum(errors) / len(errors),
        "mae": sum(absolute) / len(absolute),
        "rmse": math.sqrt(sum(value * value for value in errors) / len(errors)),
        "mape": sum(percentage) / len(percentage) if percentage else None,
        "mapeEligibleCount": len(percentage),
        "wape": sum(absolute) / denominator if denominator else None,
    }


def rolling_backtest(values, initial_history, horizon=1, model="linear", period=7):
    observations = [finite(value) for value in values]
    integer(initial_history, "initialHistory", 2, len(observations))
    integer(horizon, "horizon", 1, 365)
    actual = []
    predicted = []
    folds = []
    for origin in range(initial_history, len(observations) - horizon + 1, horizon):
        history = observations[:origin]
        target = observations[origin:origin + horizon]
        if model == "linear":
            estimates = linear_forecast(history, horizon)["predictions"]
        elif model == "holt":
            estimates = holt(history, horizon)["predictions"]
        elif model == "seasonal":
            estimates = seasonal_naive(history, period, horizon)
        else:
            raise ValueError("unknown backtest model")
        actual.extend(target)
        predicted.extend(estimates)
        folds.append({"origin": origin, "metrics": error_metrics(target, estimates)})
    if not folds:
        raise ValueError("not enough observations for one backtest fold")
    return {"model": model, "folds": folds, "metrics": error_metrics(actual, predicted)}
