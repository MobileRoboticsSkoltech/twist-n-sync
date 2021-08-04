//
// Created by achains on 23.07.2021.
//

#ifndef TWIST_N_SYNC_CPP_MODULE_TSUTIL_H
#define TWIST_N_SYNC_CPP_MODULE_TSUTIL_H

#include "Eigen/Core"

#include <vector>

namespace TSUtil {
    // Implementation of numpy.arange function
    Eigen::VectorXd arangeEigen(double start, double const & stop, double const & step = 1.0);

    // Implementation of numpy.diff function
    Eigen::VectorXd adjDiffEigen(Eigen::VectorXd const & data);

    // Implementation of scipy.signal.correlate function
    // arg1 : Eigen::VectorXd
    // arg2 : Eigen::VectorXd
    // return : Eigen::VectorXd
    Eigen::VectorXd eigenCrossCor(Eigen::VectorXd & data_1, Eigen::VectorXd & data_2);

    // Implementation of np.roots
    // Quadratic equation: coeffs[2] * x^2 + coeffs[1] * x + coeffs[0] = 0
    Eigen::Vector2d quadraticRoots(Eigen::VectorXd const & coeffs);

    Eigen::MatrixX3d vectorToEigMatrixX3d(std::vector<std::vector<double>> data);

    Eigen::VectorXd vectorToEigVectorXd(std::vector<double> data);

    Eigen::VectorXd getNormOfRows(Eigen::MatrixX3d const & data);

    // Return values of CubicSpline(x_old, y_old) at points of x_new
    Eigen::VectorXd interpolate(Eigen::VectorXd const & x_old, Eigen::VectorXd const & y_old,
                                Eigen::VectorXd const & x_new);

    struct CorrData{
        Eigen::VectorXd cross_cor;
        Eigen::Index initial_index;
    };
}


#endif //TWIST_N_SYNC_CPP_MODULE_TSUTIL_H
