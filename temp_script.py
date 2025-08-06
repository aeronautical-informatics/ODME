# Translated YAML to Python

config = {
    'Scenario': {
        'Environment': {
            'Season': {
                'Summer': None,
                'Winter': None
            },
            'TimeOfDay': {
                'Dawn': None,
                'Morning': None,
                'Noon': None,
                'Afternoon': None,
                'Dusk': None
            }
        },
        'EgoAC': None,
        'Plane_Distance': {
            'type': 'int',
            'min': 100,
            'max': 600
        },
        'Roll': {
            'type': 'int',
            'min': -30,
            'max': 30
        },
        'Pitch': {
            'type': 'int',
            'min': -30,
            'max': 30
        },
        'Yaw': {
            'type': 'int',
            'min': 0,
            'max': 360
        },
        'IntruderAC': None,
        'Latitude': {
            'type': 'double',
            'min': 63.7,
            'max': 65
        },
        'Longitude': {
            'type': 'double',
            'min': -22.6,
            'max': -21.5
        },
        'Altitude': {
            'type': 'int',
            'min': 700,
            'max': 1300
        },
        'B737_300': None,
        'B787_8': None,
        'A340_600': None,
        'abc': {
            'type': None
        }
    }
}
import random

def generate_random_scenario(cfg):
    scenario_instance = {}
    for key, val in cfg['Scenario'].items():
        if isinstance(val, dict) and 'min' in val and 'max' in val:
            min_val = val['min']
            max_val = val['max']
            scenario_instance[key] = random.uniform(min_val, max_val)
        else:
            scenario_instance[key] = val
    return scenario_instance

random_scenario = generate_random_scenario(config)
print("Random Scenario Generated:", random_scenario)
