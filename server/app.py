import sys, os
from flask import Flask

from db.init_db import init_db
from resources import product, user
from dotenv import load_dotenv

# Blueprint
app = Flask(__name__)

# DB Setup 
load_dotenv()
app.config['SQLALCHEMY_DATABASE_URI'] = os.environ.get('SQLALCHEMY_DATABASE_URI')
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

# File upload setup
PROJECT_HOME = '/workspace/firstContainer/USan'
UPLOAD_FOLDER = '{}/uploads/'.format(PROJECT_HOME)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['ALLOWED_EXTENSIONS'] = set(['png', 'jpg', 'jpeg', 'gif'])
init_db(app)


# Register the blueprint 
app.register_blueprint(product.bp)
app.register_blueprint(user.bp)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=6000, debug=True)
    